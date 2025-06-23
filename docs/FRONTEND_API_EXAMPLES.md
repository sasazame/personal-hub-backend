# フロントエンドAPI実装例

## 1. 認証API実装例（TypeScript + Axios）

### 基本設定
```typescript
import axios, { AxiosInstance } from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api/v1';

// Axiosインスタンスの作成
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // CSRF cookieのため必要
});

// リクエストインターセプター
apiClient.interceptors.request.use(
  (config) => {
    // JWTトークンの付与
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // CSRFトークンの付与
    const csrfToken = getCookie('XSRF-TOKEN');
    if (csrfToken) {
      config.headers['X-XSRF-TOKEN'] = csrfToken;
    }
    
    return config;
  },
  (error) => Promise.reject(error)
);

// レスポンスインターセプター
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // 401エラー処理
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
        // リフレッシュ失敗時はログイン画面へ
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    // 429エラー処理（Rate Limit）
    if (error.response?.status === 429) {
      const retryAfter = error.response.headers['x-rate-limit-retry-after-seconds'];
      console.error(`Rate limit exceeded. Retry after ${retryAfter} seconds`);
    }
    
    return Promise.reject(error);
  }
);
```

### 認証サービス実装
```typescript
export class AuthService {
  // ログイン
  static async login(email: string, password: string): Promise<AuthResponse> {
    const response = await apiClient.post('/auth/login', { email, password });
    const data = response.data;
    
    // トークンを保存
    localStorage.setItem('accessToken', data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    
    return data;
  }
  
  // 新規登録
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
  
  // ソーシャル認証開始
  static async initiateOAuth(provider: 'google' | 'github'): Promise<OAuthInitResponse> {
    const response = await apiClient.get(`/auth/oidc/${provider}/authorize`);
    return response.data;
  }
  
  // OAuthコールバック処理
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
  
  // ログアウト
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

## 2. React コンポーネント実装例

### ログインコンポーネント
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
        setError('メールアドレスまたはパスワードが正しくありません');
      } else if (err.response?.status === 429) {
        setError('ログイン試行回数が多すぎます。しばらくしてから再試行してください');
      } else {
        setError('ログインに失敗しました');
      }
    } finally {
      setLoading(false);
    }
  };
  
  const handleSocialLogin = async (provider: 'google' | 'github') => {
    try {
      const { authorizationUrl, state } = await AuthService.initiateOAuth(provider);
      
      // stateをセッションストレージに保存
      sessionStorage.setItem('oauth_state', state);
      sessionStorage.setItem('oauth_provider', provider);
      
      // 認証プロバイダーへリダイレクト
      window.location.href = authorizationUrl;
    } catch (err) {
      setError(`${provider}ログインの開始に失敗しました`);
    }
  };
  
  return (
    <div className="login-container">
      <h1>ログイン</h1>
      
      {error && (
        <div className="alert alert-error">{error}</div>
      )}
      
      <form onSubmit={handleSubmit}>
        <input
          type="email"
          placeholder="メールアドレス"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        
        <input
          type="password"
          placeholder="パスワード"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        
        <button type="submit" disabled={loading}>
          {loading ? 'ログイン中...' : 'ログイン'}
        </button>
      </form>
      
      <div className="social-login">
        <button 
          onClick={() => handleSocialLogin('google')}
          className="google-login-btn"
        >
          <img src="/google-icon.svg" alt="Google" />
          Googleでログイン
        </button>
        
        <button 
          onClick={() => handleSocialLogin('github')}
          className="github-login-btn"
        >
          <img src="/github-icon.svg" alt="GitHub" />
          GitHubでログイン
        </button>
      </div>
      
      <p>
        アカウントをお持ちでない方は
        <a href="/register">新規登録</a>
      </p>
    </div>
  );
};
```

### OAuthコールバックコンポーネント
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
        setError('認証がキャンセルされました');
        return;
      }
      
      if (!code || !state) {
        setError('認証パラメーターが不足しています');
        return;
      }
      
      // セッションストレージからstateを検証
      const savedState = sessionStorage.getItem('oauth_state');
      const provider = sessionStorage.getItem('oauth_provider');
      
      if (state !== savedState) {
        setError('セキュリティエラー: 不正なstate');
        return;
      }
      
      try {
        await AuthService.handleOAuthCallback(provider!, code, state);
        
        // クリーンアップ
        sessionStorage.removeItem('oauth_state');
        sessionStorage.removeItem('oauth_provider');
        
        navigate('/dashboard');
      } catch (err) {
        setError('認証処理に失敗しました');
      }
    };
    
    handleCallback();
  }, [searchParams, navigate]);
  
  return (
    <div className="oauth-callback">
      {error ? (
        <div>
          <h2>認証エラー</h2>
          <p>{error}</p>
          <a href="/login">ログイン画面に戻る</a>
        </div>
      ) : (
        <div>
          <h2>認証処理中...</h2>
          <div className="spinner"></div>
        </div>
      )}
    </div>
  );
};
```

### 認証Context（状態管理）
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
      // UserInfo エンドポイントでユーザー情報取得
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

### Protected Route実装
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

## 3. ルーティング設定例

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
          {/* 公開ルート */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/auth/callback" element={<OAuthCallback />} />
          
          {/* 保護されたルート */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/todos" element={<TodoList />} />
            <Route path="/notes" element={<NoteList />} />
            <Route path="/calendar" element={<Calendar />} />
          </Route>
          
          {/* デフォルトリダイレクト */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
```

## 4. ユーティリティ関数

```typescript
// Cookie取得
function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop()?.split(';').shift() || null;
  }
  return null;
}

// JWTデコード
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

// トークン有効期限チェック
function isTokenExpired(token: string): boolean {
  const decoded = decodeJWT(token);
  if (!decoded || !decoded.exp) return true;
  
  const currentTime = Date.now() / 1000;
  return decoded.exp < currentTime;
}
```

## テスト実装例

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