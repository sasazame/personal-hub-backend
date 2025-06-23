# フロントエンド実装依頼書

## 概要
Personal Hub バックエンドAPIに実装された認証・認可機能に対応するフロントエンド実装をお願いします。

## 実装済みバックエンド機能

### 1. OpenID Connect Core 1.0 対応
- 標準的なOIDC認証フロー
- Discovery エンドポイント
- JWKS エンドポイント
- UserInfo エンドポイント

### 2. ソーシャルログイン機能
- Google OpenID Connect認証
- GitHub OAuth認証

### 3. セキュリティ機能
- CSRF保護（XSRF-TOKEN）
- Rate Limiting
- セキュリティイベント追跡

## フロントエンド実装依頼内容

### 1. 認証関連コンポーネント

#### 1.1 ログイン画面
```typescript
// 必要な機能:
- メールアドレス/パスワードでのログイン
- "Googleでログイン" ボタン
- "GitHubでログイン" ボタン
- パスワードリセットリンク
- 新規登録へのリンク
```

#### 1.2 新規登録画面
```typescript
// 必要な機能:
- メールアドレス
- パスワード（強度チェック付き）
- パスワード確認
- 利用規約同意チェックボックス
- ソーシャル登録オプション
```

#### 1.3 ソーシャルログインフロー
```typescript
// Google/GitHub認証フロー:
1. /api/v1/auth/oidc/{provider}/authorize へGETリクエスト
2. レスポンスのauthorizationUrlにリダイレクト
3. コールバック後、codeとstateを取得
4. /api/v1/auth/oidc/{provider}/callback へPOSTリクエスト
5. JWTトークンを受け取り、localStorageに保存
```

### 2. APIクライアントの実装

#### 2.1 認証サービス
```typescript
interface AuthService {
  // 通常ログイン
  login(email: string, password: string): Promise<AuthResponse>;
  
  // 新規登録
  register(email: string, password: string, username: string): Promise<AuthResponse>;
  
  // ソーシャル認証開始
  initiateOAuth(provider: 'google' | 'github'): Promise<OAuthInitResponse>;
  
  // OAuthコールバック処理
  handleOAuthCallback(provider: string, code: string, state: string): Promise<AuthResponse>;
  
  // ログアウト
  logout(): Promise<void>;
  
  // トークンリフレッシュ
  refreshToken(): Promise<AuthResponse>;
}

interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  user: {
    id: string;
    email: string;
    username: string;
    createdAt: string;
    updatedAt: string;
  };
}

interface OAuthInitResponse {
  authorizationUrl: string;
  state: string;
  provider: string;
}
```

#### 2.2 HTTPインターセプター
```typescript
// 実装が必要な機能:
1. すべてのリクエストにJWTトークンを付与
   - Header: Authorization: Bearer {token}

2. CSRF保護
   - CookieからXSRF-TOKENを読み取り
   - Header: X-XSRF-TOKEN に設定

3. 401エラーハンドリング
   - トークンリフレッシュを試行
   - 失敗時はログイン画面へリダイレクト

4. 429エラー（Rate Limit）ハンドリング
   - エラーメッセージ表示
   - X-Rate-Limit-Retry-After-Seconds ヘッダーから待機時間取得
```

### 3. 状態管理（Redux/Zustand/Context API）

#### 3.1 認証状態
```typescript
interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  loading: boolean;
  error: string | null;
}

// 必要なアクション:
- SET_USER
- SET_TOKEN
- LOGOUT
- SET_LOADING
- SET_ERROR
```

#### 3.2 セキュリティ状態
```typescript
interface SecurityState {
  csrfToken: string | null;
  rateLimitRemaining: number | null;
  lastSecurityEvent: SecurityEvent | null;
}
```

### 4. ルーティング保護

#### 4.1 ProtectedRoute コンポーネント
```typescript
const ProtectedRoute: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }
  
  return <>{children}</>;
};
```

### 5. UI/UXコンポーネント

#### 5.1 ソーシャルログインボタン
```typescript
// Googleログインボタン
<button 
  onClick={() => handleSocialLogin('google')}
  className="google-login-button"
>
  <GoogleIcon />
  Googleでログイン
</button>

// GitHubログインボタン
<button 
  onClick={() => handleSocialLogin('github')}
  className="github-login-button"
>
  <GitHubIcon />
  GitHubでログイン
</button>
```

#### 5.2 セキュリティアラート
```typescript
// Rate Limit警告
<Alert severity="warning">
  リクエスト制限に達しました。{retryAfter}秒後に再試行してください。
</Alert>

// ログイン失敗警告
<Alert severity="error">
  ログインに失敗しました。アカウントがロックされている可能性があります。
</Alert>
```

### 6. 環境変数設定

```env
# .env.local
REACT_APP_API_BASE_URL=http://localhost:8080/api/v1
REACT_APP_OAUTH_REDIRECT_URI=http://localhost:3000/auth/callback
```

**重要**: バックエンドの起動時は以下のいずれかの方法で環境変数を設定してください：

1. **推奨方法**: 起動スクリプトを使用
   ```bash
   # Linux/Mac
   ./run.sh
   
   # Windows
   run.bat
   ```

2. **代替方法**: 環境変数を直接設定
   ```bash
   # Linux/Mac
   export GOOGLE_OIDC_CLIENT_ID=your_google_client_id
   export GOOGLE_OIDC_CLIENT_SECRET=your_google_client_secret
   mvn spring-boot:run
   
   # Windows
   set GOOGLE_OIDC_CLIENT_ID=your_google_client_id
   set GOOGLE_OIDC_CLIENT_SECRET=your_google_client_secret
   mvn spring-boot:run
   ```

### 7. 実装優先順位

1. **高優先度**
   - 基本的なログイン/ログアウト機能
   - JWTトークン管理
   - HTTPインターセプター
   - ProtectedRoute

2. **中優先度**
   - ソーシャルログイン（Google/GitHub）
   - CSRF保護
   - エラーハンドリング

3. **低優先度**
   - Rate Limit表示
   - セキュリティイベント表示
   - パスワード強度チェック

### 8. テスト要件

1. **単体テスト**
   - AuthService の各メソッド
   - HTTPインターセプター
   - 状態管理のreducer/actions

2. **統合テスト**
   - ログインフロー全体
   - ソーシャルログインフロー
   - トークンリフレッシュ

3. **E2Eテスト**
   - ログイン → 保護されたページアクセス
   - ログアウト → リダイレクト確認

### 9. 注意事項

1. **セキュリティ**
   - トークンはlocalStorageまたはsecure cookieに保存
   - XSS対策のため、トークンをDOMに露出させない
   - HTTPSでの通信を前提とする

2. **エラーハンドリング**
   - ネットワークエラー
   - 認証エラー
   - Rate Limitエラー
   - CSRF エラー

3. **パフォーマンス**
   - トークンの有効期限チェック（15分）
   - 不要なAPI呼び出しを避ける
   - ローディング状態の適切な表示

### 10. デザイン要件

- レスポンシブデザイン
- ダークモード対応
- アクセシビリティ（WCAG 2.1 AA準拠）
- ローディングスピナー/スケルトンスクリーン

## APIエンドポイント一覧

### 認証関連
- `POST /api/v1/auth/login` - ログイン
- `POST /api/v1/auth/register` - 新規登録
- `POST /api/v1/auth/logout` - ログアウト
- `POST /api/v1/auth/refresh` - トークンリフレッシュ

### ソーシャル認証
- `GET /api/v1/auth/oidc/google/authorize` - Google認証開始
- `POST /api/v1/auth/oidc/google/callback` - Googleコールバック
- `GET /api/v1/auth/oidc/github/authorize` - GitHub認証開始
- `POST /api/v1/auth/oidc/github/callback` - GitHubコールバック

### OpenID Connect
- `GET /api/v1/.well-known/openid-configuration` - Discovery
- `GET /api/v1/oauth2/jwks` - JWKS
- `GET /api/v1/oauth2/userinfo` - UserInfo

## 参考資料

- [Google OAuth 2.0 ドキュメント](https://developers.google.com/identity/protocols/oauth2)
- [GitHub OAuth ドキュメント](https://docs.github.com/en/developers/apps/building-oauth-apps)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)

---

実装に関する質問や不明点がございましたら、お気軽にお問い合わせください。