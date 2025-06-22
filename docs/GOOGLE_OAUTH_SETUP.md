# Google OAuth セットアップガイド

## 概要
Personal Hub バックエンドでGoogle OAuth認証を使用するための設定手順です。

## 前提条件
- Google Cloud Consoleアカウント
- Google Cloud プロジェクト

## セットアップ手順

### 1. Google Cloud Console設定
1. [Google Cloud Console](https://console.cloud.google.com/)にアクセス
2. プロジェクトを選択または作成
3. 「APIとサービス」→「認証情報」に移動
4. 「認証情報を作成」→「OAuth クライアント ID」を選択
5. アプリケーションの種類で「ウェブ アプリケーション」を選択
6. 以下の設定を行う：
   - **名前**: Personal Hub Backend
   - **承認済みのJavaScript生成元**: 
     - `http://localhost:8080`
     - `http://localhost:3000` (フロントエンド用)
   - **承認済みのリダイレクトURI**:
     - `http://localhost:8080/api/v1/auth/oidc/google/callback`

### 2. 環境変数の設定
プロジェクトルートに`.env`ファイルを作成し、以下の内容を設定：

```bash
# Google OAuth Configuration
GOOGLE_OIDC_CLIENT_ID=your_client_id_here
GOOGLE_OIDC_CLIENT_SECRET=your_client_secret_here
GOOGLE_OIDC_REDIRECT_URI=http://localhost:8080/api/v1/auth/oidc/google/callback
```

### 3. アプリケーション起動
```bash
# 環境変数を読み込んで起動
source .env && mvn spring-boot:run
```

または、IDE使用時は環境変数を設定してから起動してください。

## 認証フロー

### 1. 認証開始
```
GET /api/v1/auth/oidc/google/authorize
```

レスポンス:
```json
{
  "authorizationUrl": "https://accounts.google.com/o/oauth2/v2/auth?...",
  "state": "random-state-value"
}
```

### 2. Googleログイン
ユーザーをauthorizationUrlにリダイレクトし、Googleアカウントでログイン

### 3. コールバック処理
Googleからのリダイレクト後:
```
POST /api/v1/auth/oidc/google/callback
Content-Type: application/json

{
  "code": "authorization-code-from-google",
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
    "username": "user@example.com",
    "email": "user@example.com",
    "createdAt": "2023-01-01T00:00:00",
    "updatedAt": "2023-01-01T00:00:00"
  }
}
```

## トラブルシューティング

### エラー: redirect_uri_mismatch
- Google Cloud Consoleで設定したリダイレクトURIと、アプリケーションで使用しているURIが完全に一致していることを確認
- プロトコル（http/https）、ポート番号、パスが正確に一致している必要があります

### エラー: invalid_client
- CLIENT_IDとCLIENT_SECRETが正しく設定されていることを確認
- 環境変数が正しく読み込まれていることを確認

## セキュリティ注意事項
- `.env`ファイルは絶対にGitにコミットしないでください
- 本番環境では必ずHTTPSを使用してください
- CLIENT_SECRETは安全に管理し、フロントエンドには公開しないでください