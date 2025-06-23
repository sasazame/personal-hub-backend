# OAuth 統合テストガイド

## テスト準備

### 1. バックエンド起動
```bash
cd personal-hub-backend
./run.sh
```

### 2. フロントエンド起動
```bash
cd personal-hub-frontend
npm start
```

### 3. OAuth プロバイダー設定確認
- Google Cloud Console でリダイレクト URI が更新されていることを確認
- GitHub OAuth App でコールバック URL が更新されていることを確認

## テストシナリオ

### シナリオ1: Google OAuth 正常フロー

1. **ログイン画面アクセス**
   - http://localhost:3000/login にアクセス
   - 「Googleでログイン」ボタンが表示されることを確認

2. **OAuth フロー開始**
   - 「Googleでログイン」ボタンをクリック
   - ブラウザの開発者ツールでネットワークタブを開く
   - `/api/v1/auth/oidc/google/authorize` へのリクエストを確認
   - レスポンスに `authorizationUrl` と `state` が含まれることを確認

3. **Google 認証画面**
   - Google の認証画面にリダイレクトされることを確認
   - URL が `https://accounts.google.com/o/oauth2/v2/auth` で始まることを確認
   - `client_id` パラメータが正しいことを確認
   - `redirect_uri` が `http://localhost:3000/auth/callback` になっていることを確認

4. **Google でログイン**
   - Google アカウントでログイン
   - 必要に応じて権限を承認

5. **コールバック処理**
   - `http://localhost:3000/auth/callback` にリダイレクトされることを確認
   - URL に `code` と `state` パラメータが含まれることを確認
   - 「認証処理中...」の画面が表示されることを確認

6. **バックエンドとの通信**
   - 開発者ツールで `/api/v1/auth/oidc/google/callback` への POST リクエストを確認
   - リクエストボディに `code` と `state` が含まれることを確認

7. **認証成功**
   - ダッシュボード（/dashboard）にリダイレクトされることを確認
   - localStorage に `accessToken` が保存されていることを確認
   - sessionStorage から `oauth_state` と `oauth_provider` が削除されていることを確認

### シナリオ2: GitHub OAuth 正常フロー

1. 「GitHubでログイン」ボタンで同様のフローをテスト
2. GitHub の認証画面が表示されることを確認
3. コールバック後、正常にダッシュボードへ遷移することを確認

### シナリオ3: エラーケース

#### 3.1 ユーザーが認証をキャンセル
1. Google/GitHub の認証画面で「キャンセル」をクリック
2. エラー画面が表示されることを確認
3. 「ログイン画面に戻る」ボタンが機能することを確認

#### 3.2 State パラメータ不一致
1. 開発者ツールで sessionStorage の `oauth_state` を変更
2. コールバック時にエラーが表示されることを確認

#### 3.3 ネットワークエラー
1. バックエンドを停止した状態でテスト
2. 適切なエラーメッセージが表示されることを確認

## デバッグ方法

### ブラウザ開発者ツール

1. **Console タブ**
   ```javascript
   // SessionStorage の確認
   console.log(sessionStorage.getItem('oauth_state'));
   console.log(sessionStorage.getItem('oauth_provider'));
   
   // LocalStorage の確認
   console.log(localStorage.getItem('accessToken'));
   ```

2. **Network タブ**
   - OAuth 関連のリクエストをフィルタ: `oidc|oauth`
   - 各リクエストのヘッダーとレスポンスを確認

3. **Application タブ**
   - Storage → Session Storage でセッション情報を確認
   - Storage → Local Storage でトークンを確認
   - Cookies で XSRF-TOKEN を確認

### バックエンドログ

```bash
# アプリケーションログを確認
tail -f app.log | grep -E "(OAuth|OIDC|Google|GitHub)"

# State 検証のログを確認
tail -f app.log | grep "OAuth state"
```

## チェックリスト

- [ ] Google OAuth でログインできる
- [ ] GitHub OAuth でログインできる
- [ ] エラー時に適切なメッセージが表示される
- [ ] トークンが localStorage に保存される
- [ ] sessionStorage がクリーンアップされる
- [ ] ダッシュボードへ正常に遷移する
- [ ] CORS エラーが発生しない
- [ ] State 検証が正常に動作する
- [ ] Rate Limiting が適切に動作する
- [ ] ログアウト後、再度ログインできる

## よくある問題

### 1. CORS エラー
```
Access to XMLHttpRequest at 'http://localhost:8080/...' from origin 'http://localhost:3000' has been blocked by CORS policy
```
**解決方法**: バックエンドの CORS 設定を確認

### 2. 401 Unauthorized
```
POST http://localhost:8080/api/v1/auth/oidc/google/callback 401
```
**解決方法**: State パラメータの検証エラー、sessionStorage を確認

### 3. リダイレクト URI エラー
```
Error 400: redirect_uri_mismatch
```
**解決方法**: OAuth プロバイダーの設定を確認、正確な URI を登録

### 4. Client ID が空
```
Missing required parameter: client_id
```
**解決方法**: バックエンドの環境変数を確認、アプリケーションを再起動