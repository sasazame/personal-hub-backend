# Frontend API Implementation Examples

## 1. Authentication API Implementation (TypeScript + Axios)

### Basic Setup
```typescript
import axios, { AxiosInstance } from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api/v1';

// Create Axios instance
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Required for CSRF cookies
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add JWT token
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Add CSRF token
    const csrfToken = getCookie('XSRF-TOKEN');
    if (csrfToken) {
      config.headers['X-XSRF-TOKEN'] = csrfToken;
    }
    
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // Handle 401 errors
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken,
        });
        
        const { accessToken } = response.data;
        localStorage.setItem('accessToken', accessToken);
        
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Redirect to login on refresh failure
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    // Handle 429 errors (Rate Limit)
    if (error.response?.status === 429) {
      const retryAfter = error.response.headers['x-rate-limit-retry-after-seconds'];
      console.error(`Rate limit exceeded. Retry after ${retryAfter} seconds`);
    }
    
    return Promise.reject(error);
  }
);
```

### Authentication Service Implementation
```typescript
export class AuthService {
  // Login
  static async login(email: string, password: string): Promise<AuthResponse> {
    const response = await apiClient.post('/auth/login', { email, password });
    const data = response.data;
    
    // Store tokens
    localStorage.setItem('accessToken', data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    
    return data;
  }
  
  // Registration
  static async register(email: string, password: string, username: string): Promise<AuthResponse> {
    const response = await apiClient.post('/auth/register', { 
      email, 
      password, 
      username 
    });
    const data = response.data;
    
    localStorage.setItem('accessToken', data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    
    return data;
  }
  
  // Initiate social authentication
  static async initiateOAuth(provider: 'google' | 'github'): Promise<OAuthInitResponse> {
    const response = await apiClient.get(`/auth/oidc/${provider}/authorize`);
    return response.data;
  }
  
  // Handle OAuth callback
  static async handleOAuthCallback(
    provider: string, 
    code: string, 
    state: string
  ): Promise<AuthResponse> {
    const response = await apiClient.post(`/auth/oidc/${provider}/callback`, {
      code,
      state,
    });
    const data = response.data;
    
    localStorage.setItem('accessToken', data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    
    return data;
  }
  
  // Logout
  static async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout');
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      window.location.href = '/login';
    }
  }
}
```

## 2. React Component Implementation Examples

### Login Component
```tsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthService } from '../services/AuthService';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      await AuthService.login(email, password);
      navigate('/dashboard');
    } catch (err: any) {
      if (err.response?.status === 401) {
        setError('Invalid email address or password');
      } else if (err.response?.status === 429) {
        setError('Too many login attempts. Please try again later');
      } else {
        setError('Login failed');
      }
    } finally {
      setLoading(false);
    }
  };
  
  const handleSocialLogin = async (provider: 'google' | 'github') => {
    try {
      const { authorizationUrl, state } = await AuthService.initiateOAuth(provider);
      
      // Store state in session storage
      sessionStorage.setItem('oauth_state', state);
      sessionStorage.setItem('oauth_provider', provider);
      
      // Redirect to authentication provider
      window.location.href = authorizationUrl;
    } catch (err) {
      setError(`Failed to initiate ${provider} login`);
    }
  };
  
  return (
    <div className="login-container">
      <h1>Login</h1>
      
      {error && (
        <div className="alert alert-error">{error}</div>
      )}
      
      <form onSubmit={handleSubmit}>
        <input
          type="email"
          placeholder="Email address"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        
        <button type="submit" disabled={loading}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
      
      <div className="social-login">
        <button 
          onClick={() => handleSocialLogin('google')}
          className="google-login-btn"
        >
          <img src="/google-icon.svg" alt="Google" />
          Login with Google
        </button>
        
        <button 
          onClick={() => handleSocialLogin('github')}
          className="github-login-btn"
        >
          <img src="/github-icon.svg" alt="GitHub" />
          Login with GitHub
        </button>
      </div>
      
      <p>
        Don't have an account?{' '}
        <a href="/register">Sign up</a>
      </p>
    </div>
  );
};
```

### OAuth Callback Component
```tsx
import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AuthService } from '../services/AuthService';

export const OAuthCallback: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState('');
  
  useEffect(() => {
    const handleCallback = async () => {
      const code = searchParams.get('code');
      const state = searchParams.get('state');
      const error = searchParams.get('error');
      
      if (error) {
        setError('Authentication was cancelled');
        return;
      }
      
      if (!code || !state) {
        setError('Missing authentication parameters');
        return;
      }
      
      // Verify state from session storage
      const savedState = sessionStorage.getItem('oauth_state');
      const provider = sessionStorage.getItem('oauth_provider');
      
      if (state !== savedState) {
        setError('Security error: Invalid state');
        return;
      }
      
      try {
        await AuthService.handleOAuthCallback(provider!, code, state);
        
        // Cleanup
        sessionStorage.removeItem('oauth_state');
        sessionStorage.removeItem('oauth_provider');
        
        navigate('/dashboard');
      } catch (err) {
        setError('Authentication processing failed');
      }
    };
    
    handleCallback();
  }, [searchParams, navigate]);
  
  return (
    <div className="oauth-callback">
      {error ? (
        <div>
          <h2>Authentication Error</h2>
          <p>{error}</p>
          <a href="/login">Return to login</a>
        </div>
      ) : (
        <div>
          <h2>Processing authentication...</h2>
          <div className="spinner"></div>
        </div>
      )}
    </div>
  );
};
```

### Authentication Context (State Management)
```tsx
import React, { createContext, useContext, useState, useEffect } from 'react';
import { AuthService } from '../services/AuthService';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  checkAuth: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  
  const checkAuth = async () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      setLoading(false);
      return;
    }
    
    try {
      // Get user info from UserInfo endpoint
      const response = await apiClient.get('/oauth2/userinfo');
      setUser(response.data);
    } catch (err) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    checkAuth();
  }, []);
  
  const login = async (email: string, password: string) => {
    const response = await AuthService.login(email, password);
    setUser(response.user);
  };
  
  const logout = async () => {
    await AuthService.logout();
    setUser(null);
  };
  
  return (
    <AuthContext.Provider value={{
      user,
      isAuthenticated: !!user,
      loading,
      login,
      logout,
      checkAuth,
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

### Protected Route Implementation
```tsx
import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, loading } = useAuth();
  
  if (loading) {
    return <div className="loading-spinner">Loading...</div>;
  }
  
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};
```

## 3. Routing Configuration Example

```tsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { OAuthCallback } from './pages/OAuthCallback';
import { Dashboard } from './pages/Dashboard';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/auth/callback" element={<OAuthCallback />} />
          
          {/* Protected routes */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/todos" element={<TodoList />} />
            <Route path="/notes" element={<NoteList />} />
            <Route path="/calendar" element={<Calendar />} />
          </Route>
          
          {/* Default redirect */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
```

## 4. Utility Functions

```typescript
// Get cookie
function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop()?.split(';').shift() || null;
  }
  return null;
}

// Decode JWT
function decodeJWT(token: string): any {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

// Check token expiration
function isTokenExpired(token: string): boolean {
  const decoded = decodeJWT(token);
  if (!decoded || !decoded.exp) return true;
  
  const currentTime = Date.now() / 1000;
  return decoded.exp < currentTime;
}
```

## Test Implementation Example

```typescript
// AuthService.test.ts
import { AuthService } from './AuthService';
import axios from 'axios';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('AuthService', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  });
  
  test('login should store tokens', async () => {
    const mockResponse = {
      data: {
        accessToken: 'test-access-token',
        refreshToken: 'test-refresh-token',
        user: { id: '123', email: 'test@example.com' }
      }
    };
    
    mockedAxios.post.mockResolvedValueOnce(mockResponse);
    
    const result = await AuthService.login('test@example.com', 'password');
    
    expect(localStorage.getItem('accessToken')).toBe('test-access-token');
    expect(localStorage.getItem('refreshToken')).toBe('test-refresh-token');
    expect(result).toEqual(mockResponse.data);
  });
});
```

## TypeScript Type Definitions

```typescript
interface User {
  id: string;
  username: string;
  email: string;
  createdAt: string;
  updatedAt: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  user: User;
}

interface OAuthInitResponse {
  authorizationUrl: string;
  state: string;
  provider: string;
}

interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

This implementation provides a comprehensive foundation for frontend integration with the Personal Hub backend API, including proper authentication handling, OAuth flows, and error management.