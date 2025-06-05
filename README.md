# TODO App Backend

[![CI Pipeline](https://github.com/sasazame/todo-app-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/sasazame/todo-app-backend/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/sasazame/todo-app-backend/branch/main/graph/badge.svg)](https://codecov.io/gh/sasazame/todo-app-backend)

Spring Boot + PostgreSQL で構築されたTODOアプリケーションのバックエンドAPI

## 🚀 プロジェクト概要

### 技術スタック
- **Java**: 21 (OpenJDK)
- **フレームワーク**: Spring Boot 3.3.7
- **データベース**: PostgreSQL 16+
- **ビルドツール**: Maven 3.8+
- **アーキテクチャ**: ヘキサゴナルアーキテクチャ（ポート&アダプター）

### 主な機能
- ✅ **認証・認可**: JWT ベースの認証システム
- ✅ **ユーザー管理**: ユーザー登録・ログイン機能
- ✅ **TODO管理**: CRUD 操作（作成・取得・更新・削除）
- ✅ **アクセス制御**: ユーザーは自分のTODOのみアクセス可能
- ✅ **ステータス管理**: TODO/進行中/完了
- ✅ **優先度設定**: 高/中/低
- ✅ **期限日設定**: 日付指定での期限管理
- ✅ **ページネーション**: 大量データの効率的な取得
- ✅ **セキュリティ**: エンドポイント別アクセス制御・CORS設定
- ✅ **RESTful API**: 標準的なHTTPメソッドとステータスコード
- ✅ **バリデーション**: 入力データの検証
- ✅ **グローバル例外ハンドリング**: 統一されたエラーレスポンス

## 📋 目次
1. [クイックスタート](#クイックスタート)
2. [プロジェクト構造](#プロジェクト構造)
3. [環境構築](#環境構築)
4. [API 仕様](#api-仕様)
5. [開発ガイド](#開発ガイド)
6. [テスト](#テスト)
7. [設計資料](#設計資料)

## ⚡ クイックスタート

### 前提条件
- Java 21+
- Maven 3.8+
- PostgreSQL 16+

### 起動手順
```bash
# 1. リポジトリをクローン
git clone https://github.com/sasazame/todo-app-backend.git
cd todo-app-backend

# 2. データベース設定
sudo -u postgres psql -c "CREATE DATABASE todoapp;"
sudo -u postgres psql -c "CREATE USER todoapp WITH ENCRYPTED PASSWORD 'todoapp';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE todoapp TO todoapp;"

# 3. アプリケーション起動
mvn spring-boot:run

# 4. 動作確認（ユーザー登録）
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'

# 5. ログインしてJWTトークン取得
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

アプリケーションは http://localhost:8080 で起動します。

## 🏗️ プロジェクト構造

本プロジェクトは **ヘキサゴナルアーキテクチャ** に基づいて設計されています。

```
src/main/java/com/zametech/personalhub/
├── common/           # 共通コンポーネント
├── domain/           # ドメイン層（ビジネスルール）
├── application/      # アプリケーション層（ユースケース）
├── infrastructure/   # インフラ層（外部システム連携）
└── presentation/     # プレゼンテーション層（API）
```

### 📚 詳細ドキュメント
- **[📁 フォルダ構成](docs/FOLDER_STRUCTURE.md)** - 各フォルダの目的と使用例
- **[🏛️ アーキテクチャ](docs/ARCHITECTURE.md)** - 設計思想とレイヤー構成
- **[📖 API仕様](docs/API.md)** - REST APIの詳細仕様

### 主な特徴
- **依存性の制御**: 各層の責務が明確に分離
- **拡張性**: 新機能追加時の影響範囲を最小化
- **テストしやすさ**: 各層を独立してテスト可能
- **保守性**: ビジネスロジックとインフラの分離

## 🛠️ 環境構築

### データベースセットアップ
```bash
# PostgreSQL インストール（Ubuntu/Debian）
sudo apt update
sudo apt install postgresql postgresql-contrib

# データベース・ユーザー作成
sudo -u postgres psql << EOF
CREATE DATABASE todoapp;
CREATE USER todoapp WITH ENCRYPTED PASSWORD 'todoapp';
GRANT ALL PRIVILEGES ON DATABASE todoapp TO todoapp;
ALTER DATABASE todoapp OWNER TO todoapp;
\q
EOF
```

### 設定ファイル
主要な設定は `src/main/resources/application.yml` に記載:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/todoapp
    username: todoapp
    password: todoapp
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

## 📡 API 仕様

### ベース URL
```
http://localhost:8080/api/v1
```

### 主要エンドポイント

#### 認証エンドポイント（認証不要）
| メソッド | エンドポイント | 説明 |
|---------|---------------|------|
| POST | `/auth/register` | ユーザー登録 |
| POST | `/auth/login` | ログイン |

#### TODOエンドポイント（認証必須）
| メソッド | エンドポイント | 説明 |
|---------|---------------|------|
| POST | `/todos` | TODO作成 |
| GET | `/todos` | TODO一覧取得（ページング） |
| GET | `/todos/{id}` | TODO取得（ID指定） |
| GET | `/todos?status={status}` | ステータス別TODO取得 |
| PUT | `/todos/{id}` | TODO更新 |
| DELETE | `/todos/{id}` | TODO削除 |

**注意**: TODOエンドポイントにアクセスするには、Authorizationヘッダーに`Bearer {token}`形式でJWTトークンを含める必要があります。

### リクエスト例
```bash
# 1. ユーザー登録
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "山田",
    "lastName": "太郎"
  }'

# 2. ログイン（レスポンスからaccessTokenを取得）
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'

# 3. TODO作成（認証必須）
curl -X POST http://localhost:8080/api/v1/todos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "プロジェクト完了",
    "description": "最終レビューと提出",
    "priority": "HIGH",
    "dueDate": "2024-12-31"
  }'

# 4. TODO一覧取得（認証必須）
curl "http://localhost:8080/api/v1/todos?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

詳細なAPI仕様は [docs/API.md](docs/API.md) を参照してください。

## 👨‍💻 開発ガイド

### ブランチ戦略
```bash
# 新機能開発
git checkout -b feat/feature-name
# 実装・テスト・コミット
git commit -m "feat: 新機能の説明"
# プルリクエスト作成
git push origin feat/feature-name
gh pr create --assignee sasazame
```

### コーディング規約
- **Java**: Google Java Style Guide
- **コミットメッセージ**: Conventional Commits
- **テスト**: JUnit 5 + Mockito

### プロジェクト構造
```
src/main/java/com/example/todoapp/
├── common/              # 共通コンポーネント
│   ├── config/         # 設定クラス（SecurityConfig等）
│   ├── exception/      # 例外ハンドリング
│   ├── util/           # ユーティリティ
│   └── validation/     # バリデーションロジック
├── domain/              # ドメイン層
│   ├── model/          # ドメインモデル（Todo, User等）
│   └── repository/     # リポジトリインターフェース
├── application/         # アプリケーション層
│   ├── dto/            # アプリケーション層DTO
│   └── service/        # ビジネスロジック
├── infrastructure/      # インフラストラクチャ層
│   ├── persistence/    # データアクセス（JPA実装）
│   └── security/       # セキュリティ関連（JWT処理等）
└── presentation/        # プレゼンテーション層
    ├── controller/     # REST コントローラー
    ├── dto/            # API リクエスト/レスポンス DTO
    └── mapper/         # DTO ↔ ドメインモデル変換
```

## 🧪 テスト

### テスト実行
```bash
# 全テスト実行
mvn test

# 統合テスト実行
mvn verify

# カバレッジレポート生成
mvn test jacoco:report
```

### テスト構成
- **単体テスト**: Service 層のビジネスロジック（JUnit 5 + Mockito）
- **統合テスト**: 認証・認可を含む E2E テスト（SpringBootTest）
- **API テスト**: Controller 層のエンドポイント（MockMvc）
- **セキュリティテスト**: JWT認証・アクセス制御のテスト

### テスト環境
- **データベース**: H2 In-Memory（テスト専用）
- **設定**: `application-test.yml`での専用設定
- **マイグレーション**: テスト用Flywayスクリプト

## 📚 設計資料

### 詳細ドキュメント
- **[🏛️ アーキテクチャ設計](docs/ARCHITECTURE.md)** - システム全体の設計思想とレイヤー構成
- **[📁 フォルダ構成](docs/FOLDER_STRUCTURE.md)** - プロジェクト構造と各フォルダの目的
- **[📖 API仕様書](docs/API.md)** - REST APIの詳細仕様
- **[🗄️ データベース設計](docs/DATABASE.md)** - DB スキーマと設計方針

### アーキテクチャ概要
```
┌─────────────────┐
│ Presentation    │ ← REST API, DTO
├─────────────────┤
│ Application     │ ← ビジネスロジック
├─────────────────┤
│ Domain          │ ← ドメインモデル
├─────────────────┤
│ Infrastructure  │ ← データアクセス, 外部連携
└─────────────────┘
```

## 🔧 ツール・ライブラリ

### 主要依存関係
- **Spring Boot Starter Web** - REST API
- **Spring Boot Starter Data JPA** - データアクセス
- **Spring Boot Starter Security** - セキュリティ・認証
- **Spring Boot Starter Validation** - バリデーション
- **PostgreSQL Driver** - データベース接続
- **Flyway** - データベースマイグレーション
- **JJWT** - JWT トークン処理
- **Lombok** - ボイラープレートコード削減
- **H2 Database** - テスト用インメモリDB

### 開発ツール
- **Spring Boot DevTools** - ホットリロード
- **Spring Boot Actuator** - 監視・メトリクス

## 🚧 今後の開発予定

### 近期予定
- [ ] カテゴリー・タグ機能
- [ ] 検索・フィルタリング機能強化
- [ ] ファイル添付機能
- [ ] 通知・リマインダー機能

### 中長期予定
- [ ] キャッシュ機能（Redis）
- [ ] 一括操作 API
- [ ] OpenAPI ドキュメント自動生成
- [ ] パフォーマンス監視・メトリクス

## 📝 ライセンス

このプロジェクトは MIT ライセンスの下で公開されています。

## 🤝 貢献

1. このリポジトリをフォーク
2. feature ブランチを作成 (`git checkout -b feat/amazing-feature`)
3. 変更をコミット (`git commit -m 'feat: 素晴らしい機能を追加'`)
4. ブランチにプッシュ (`git push origin feat/amazing-feature`)
5. プルリクエストを作成

## 📞 サポート

質問や問題がある場合は、[Issues](https://github.com/sasazame/todo-app-backend/issues) を作成してください。

---

**開発者**: sasazame  
**最終更新**: 2025年5月