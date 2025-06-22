# GitHub OAuth セットアップガイド

## 概要
Personal Hub バックエンドでGitHub OAuth認証を使用するための設定手順です。

## 前提条件
- GitHubアカウント
- GitHub OAuthアプリケーション

## セットアップ手順

### 1. GitHub OAuthアプリケーション作成
1. GitHubにログイン
2. Settings → Developer settings → OAuth Appsに移動
3. 「New OAuth App」または「Register a new application」をクリック
4. 以下の情報を入力：
   - **Application name**: Personal Hub Backend
   - **Homepage URL**: http://localhost:8080
   - **Application description**: Personal Hub OAuth authentication
   - **Authorization callback URL**: http://localhost:8080/api/v1/auth/oidc/github/callback
5. 「Register application」をクリック
6. Client IDとClient Secretをメモ（Client Secretは「Generate a new client secret」で生成）

### 2. 環境変数の設定
プロジェクトルートの`.env`ファイルに以下を追加：

```bash
# GitHub OAuth Configuration
GITHUB_CLIENT_ID=your_github_client_id_here
GITHUB_CLIENT_SECRET=your_github_client_secret_here
GITHUB_REDIRECT_URI=http://localhost:8080/api/v1/auth/oidc/github/callback
```

### 3. アプリケーション起動
```bash
# 環境変数を読み込んで起動
source .env && mvn spring-boot:run
```

## 認証フロー

### 1. 認証開始
```
GET /api/v1/auth/oidc/github/authorize
```

レスポンス:
```json
{
  "authorizationUrl": "https://github.com/login/oauth/authorize?...",
  "state": "random-state-value",
  "provider": "github"
}
```

### 2. GitHubログイン
ユーザーをauthorizationUrlにリダイレクトし、GitHubアカウントでログイン

### 3. コールバック処理
GitHubからのリダイレクト後:
```
POST /api/v1/auth/oidc/github/callback
Content-Type: application/json

{
  "code": "authorization-code-from-github",
  "state": "state-from-step-1"
}
```

レスポンス:
```json
{
  "accessToken": "jwt-token",
  "refreshToken": null,
  "user": {
    "id": "user-uuid",
    "username": "github-username",
    "email": "user@example.com",
    "createdAt": "2023-01-01T00:00:00",
    "updatedAt": "2023-01-01T00:00:00"
  }
}
```

## GitHub OAuthで取得される情報

### ユーザー基本情報
- GitHubユーザーID
- ユーザー名（login）
- 名前（フルネーム）
- メールアドレス（プライマリ＋検証済み）
- アバター画像URL

### 追加プロフィール情報（保存のみ）
- 会社名
- ブログURL
- 所在地
- 自己紹介（bio）
- パブリックリポジトリ数
- フォロワー数
- フォロー数

## トラブルシューティング

### エラー: redirect_uri_mismatch
- GitHubアプリケーション設定の「Authorization callback URL」が正確に一致していることを確認
- プロトコル（http/https）、ポート番号、パスが完全に一致している必要があります

### エラー: bad_verification_code
- 認可コードの有効期限切れ（10分）
- 認可コードが既に使用済み
- 不正な認可コード

### エラー: incorrect_client_credentials
- CLIENT_IDとCLIENT_SECRETが正しく設定されていることを確認
- 環境変数が正しく読み込まれていることを確認

## セキュリティ注意事項
- `.env`ファイルは絶対にGitにコミットしないでください
- 本番環境では必ずHTTPSを使用してください
- CLIENT_SECRETは安全に管理し、フロントエンドには公開しないでください
- GitHubアプリケーションの設定で適切なスコープのみを要求してください

## 開発環境と本番環境の設定

### 開発環境
```
Authorization callback URL: http://localhost:8080/api/v1/auth/oidc/github/callback
```

### 本番環境
```
Authorization callback URL: https://yourdomain.com/api/v1/auth/oidc/github/callback
```

複数の環境で使用する場合は、環境ごとに別のGitHub OAuthアプリケーションを作成することを推奨します。