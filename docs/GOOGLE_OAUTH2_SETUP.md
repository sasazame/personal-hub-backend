# Google OAuth2 Setup Guide

## Google Cloud Console設定

### 1. OAuth2認証情報の確認

1. [Google Cloud Console](https://console.cloud.google.com/)にアクセス
2. 「APIとサービス」→「認証情報」を選択
3. OAuth 2.0 クライアント IDを確認

### 2. リダイレクトURIの設定

**承認済みのリダイレクトURI**に以下を追加：
```
http://localhost:3000/auth/callback
```

### 3. OAuth同意画面の設定

1. 「APIとサービス」→「OAuth同意画面」を選択
2. 以下の項目を確認：
   - **公開ステータス**: テスト中の場合は「テスト」
   - **テストユーザー**: 開発中はテストユーザーを追加
   - **スコープ**: 以下が含まれていることを確認
     - `openid`
     - `email`
     - `profile`
     - `https://www.googleapis.com/auth/calendar`

### 4. 403エラーの対処法

#### 原因1: テストユーザーの未登録
- OAuth同意画面で「テストユーザー」にあなたのGoogleアカウントを追加

#### 原因2: アプリが「テスト」ステータス
- 開発中は問題ありません
- 本番環境では「本番」に変更が必要

#### 原因3: カレンダースコープの未承認
- OAuth同意画面で`https://www.googleapis.com/auth/calendar`スコープを追加
- 「機密性の高いスコープ」として追加の確認が必要な場合があります

## フロントエンド・バックエンド連携フロー

1. **フロントエンド**
   - ユーザーが「Googleでログイン」をクリック
   - バックエンドの`/api/v1/auth/oidc/google/authorize`を呼び出してauthorization URLを取得
   - Googleの認証画面にリダイレクト

2. **Google**
   - ユーザーが認証・承認
   - `http://localhost:3000/auth/callback`にリダイレクト（codeパラメータ付き）

3. **フロントエンド**
   - callbackページでcodeを受け取る
   - バックエンドの`/api/v1/auth/oidc/google/callback`にPOSTリクエストでcodeを送信

4. **バックエンド**
   - codeをアクセストークンに交換
   - JWTトークンを生成して返却

## トラブルシューティング

### 403 access_deniedエラー

1. **Google Cloud Consoleで確認**
   ```
   - クライアントID: 963396018097-14j0r3omfpfjp4b57m6v74dku42gmq9d.apps.googleusercontent.com
   - リダイレクトURI: http://localhost:3000/auth/callback が登録されているか
   - OAuth同意画面が適切に設定されているか
   ```

2. **テストユーザーの追加**
   - OAuth同意画面 → テストユーザー → 「+ユーザーを追加」
   - 使用するGoogleアカウントのメールアドレスを追加

3. **ブラウザのキャッシュクリア**
   - 過去の認証情報が残っている可能性があります
   - シークレットウィンドウで試してみてください

4. **スコープの確認**
   - カレンダースコープ（`https://www.googleapis.com/auth/calendar`）が機密スコープとして扱われる場合があります
   - OAuth同意画面で明示的に追加が必要です

## 開発環境での推奨設定

```bash
# .env ファイル
GOOGLE_OIDC_CLIENT_ID=your-client-id
GOOGLE_OIDC_CLIENT_SECRET=your-client-secret
GOOGLE_OIDC_REDIRECT_URI=http://localhost:3000/auth/callback
```

## セキュリティ注意事項

1. **クライアントシークレット**
   - 本番環境では環境変数で管理
   - Gitにコミットしない

2. **リダイレクトURI**
   - 本番環境では必ずHTTPSを使用
   - 正確なURIのみを登録（ワイルドカード不可）

3. **スコープ**
   - 必要最小限のスコープのみ要求
   - カレンダースコープは機密性が高いため、利用目的を明確に