# Claude Code 協働開発ガイドライン

## プロジェクト概要
Personal Hub 統合アプリケーション - バックエンド API
- **技術スタック**: Spring Boot 3.3+, Java 21, PostgreSQL, Maven
- **アーキテクチャ**: ヘキサゴナルアーキテクチャ
- **目的**: 堅牢でスケーラブルなRESTful 個人用ハブ管理API

## 開発フロー（重要）
```bash
# 1. 新機能開発時はfeatブランチを作成
git checkout -b feat/feature-name

# 2. 実装・テスト・コミット
git add . && git commit -m "feat: 機能の説明"

# 3. GitHubにプッシュしてPRを作成（宛先: sasazame）
git push origin feat/feature-name
gh pr create --title "機能タイトル" --body "詳細説明" --assignee sasazame
```

## コーディング規約

### Java
- Google Java Style Guide準拠
- メソッドは20行以内、単一責任
- 早期リターン、Optional活用
- Record使用でイミュータブルDTO

### Spring Boot
- コンストラクタインジェクション（`@RequiredArgsConstructor`）
- レイヤー責務分離:
  - Controller: リクエスト/レスポンス変換、バリデーション
  - Service: ビジネスロジック、トランザクション
  - Repository: データアクセスのみ
- DTOとEntity必須分離

### データベース
- Flyway使用、snake_case命名
- 外部キー制約必須、適切なインデックス

## コミット規約
```
<type>(<scope>): <subject>

<body>

🤖 Generated with [Claude Code](https://claude.ai/code)
Co-Authored-By: Claude <noreply@anthropic.com>
```

**タイプ**: feat, fix, docs, style, refactor, perf, test, chore, build, ci

## テスト方針
- **単体テスト**: JUnit 5 + Mockito（Service層中心）
- **統合テスト**: @SpringBootTest + MockMvc + H2
- **セキュリティテスト**: JWT認証・認可動作検証
- **APIテスト**: 認証込みエンドポイントテスト
- **カバレッジ**: 80%以上（Service層90%以上）

### テスト環境設定
- **DB**: H2 In-Memory（専用テスト用Flyway migrations）
- **設定**: `application-test.yml`
- **認証**: テスト用JWT設定

## アーキテクチャ構造
**ヘキサゴナルアーキテクチャ** を採用し、各層の責務を明確に分離。

```
src/main/java/com/zametech/todoapp/
├── common/           # 共通コンポーネント
├── domain/           # ドメイン層（ビジネスルール）
├── application/      # アプリケーション層（ユースケース）
├── infrastructure/   # インフラ層（外部システム連携）
└── presentation/     # プレゼンテーション層（API）
```

**📚 詳細**: [フォルダ構成ガイド](docs/FOLDER_STRUCTURE.md) 参照

## API仕様
- **ベースURL**: `/api/v1`
- **認証**: JWT Bearer Token（一部エンドポイントを除く）
- **認証不要**: `/auth/register`, `/auth/login`
- **認証必須**: `/todos/**`, `/calendar/**`, `/notes/**`, `/analytics/**`, `/users/**`（ユーザーは自分のデータのみアクセス可能）
- **CORS**: localhost:3000許可、適切なヘッダー設定
- **エラーレスポンス**: 統一形式（401, 403, 404等）

## セキュリティ
- **認証**: JWT Bearer Token（JJWT 0.12.5使用）
- **認可**: エンドポイント別アクセス制御
- **パスワード**: BCrypt暗号化
- **アクセス制御**: ユーザーは自分のデータのみアクセス可能
- **CORS**: localhost:3000許可、適切なヘッダー設定

## Claude Codeへの依頼テンプレート
```markdown
## 実装したい機能
[具体的な機能説明]

## 現在の状況
[関連ファイル、既存実装]

## 期待する結果
[APIレスポンス例、動作説明]

## 制約事項
[パフォーマンス要件、セキュリティ考慮]
```

## 開発時チェックリスト
- [ ] featブランチで作業
- [ ] レイヤー責務分離
- [ ] DTOとEntity分離
- [ ] 適切な例外処理
- [ ] バリデーション実装
- [ ] ログ出力適切
- [ ] テスト作成
- [ ] PR作成（assignee: sasazame）

## 環境・設定
- Java 21, Maven 3.8+, PostgreSQL 16+
- application.yml: 詳細は`docs/`参照
- DB接続: localhost:5432/personalhub (personalhub/personalhub)
- ディレクトリ名: personal-hub-backend（todo-app-backendからフォーク）

## 重要な実装パターン
1. **Repository**: インターフェース/実装分離
2. **Service**: `@Transactional`でトランザクション管理
3. **Controller**: `@Valid`でバリデーション
4. **Exception**: グローバルハンドラー使用
5. **DTO**: Recordで不変オブジェクト

このファイルはClaude Codeが効率的に作業するための簡潔なガイドライン。
詳細な設計情報は`README.md`および`docs/`フォルダーを参照。