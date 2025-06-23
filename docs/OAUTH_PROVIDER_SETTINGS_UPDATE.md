# OAuth プロバイダー設定更新ガイド

フロントエンドへのリダイレクトに対応するため、Google Cloud Console と GitHub の OAuth アプリ設定を更新する必要があります。

## Google Cloud Console の設定更新

1. [Google Cloud Console](https://console.cloud.google.com/) にアクセス

2. プロジェクトを選択

3. 左側メニューから「APIとサービス」→「認証情報」を選択

4. OAuth 2.0 クライアント ID から該当のクライアントをクリック

5. 「承認済みのリダイレクト URI」セクションで以下を更新：

   **削除する URI:**
   ```
   http://localhost:8080/api/v1/auth/oidc/google/callback
   ```

   **追加する URI:**
   ```
   http://localhost:3000/auth/callback
   http://localhost:3000/auth/google/callback
   ```

6. 「保存」をクリック

## GitHub OAuth Apps の設定更新

1. [GitHub Settings](https://github.com/settings/developers) にアクセス

2. 「OAuth Apps」を選択

3. 該当の OAuth App をクリック

4. 「Authorization callback URL」を以下に更新：
   ```
   http://localhost:3000/auth/callback
   ```

5. 「Update application」をクリック

## 本番環境用の設定

本番環境では以下のような URL に変更してください：

### Google Cloud Console
```
https://yourdomain.com/auth/callback
https://yourdomain.com/auth/google/callback
```

### GitHub
```
https://yourdomain.com/auth/callback
```

## 設定変更後の注意事項

1. **キャッシュの影響**
   - 設定変更が反映されるまで数分かかる場合があります
   - ブラウザのキャッシュをクリアすることを推奨

2. **複数環境の管理**
   - 開発環境と本番環境で異なるクライアントIDを使用することを推奨
   - 環境ごとに別々の OAuth アプリを作成

3. **セキュリティ**
   - リダイレクト URI は完全一致である必要があります
   - ワイルドカードは使用できません
   - HTTPS を使用することを強く推奨（localhost は例外）

## トラブルシューティング

### 「リダイレクト URI が一致しません」エラー

1. URL のスキーム（http/https）が正しいか確認
2. ポート番号が正しいか確認（:3000）
3. パスが完全に一致しているか確認
4. 末尾のスラッシュの有無を確認

### 設定が反映されない場合

1. ブラウザのキャッシュをクリア
2. シークレット/プライベートモードで試す
3. 5-10分待ってから再試行

## 環境変数の更新確認

バックエンドの `.env` ファイルが以下のように更新されていることを確認：

```env
# Google OAuth Configuration
GOOGLE_OIDC_CLIENT_ID=your_google_client_id
GOOGLE_OIDC_CLIENT_SECRET=your_google_client_secret
GOOGLE_OIDC_REDIRECT_URI=http://localhost:3000/auth/callback

# GitHub OAuth Configuration
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_REDIRECT_URI=http://localhost:3000/auth/callback
```