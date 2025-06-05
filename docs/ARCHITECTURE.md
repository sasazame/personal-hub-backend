# アーキテクチャ設計書

## 概要
本プロジェクトは、ヘキサゴナルアーキテクチャ（ポート&アダプターパターン）を採用したSpring Boot製Personal Hub統合アプリケーションです。TODO管理、カレンダー、ノート、分析機能を統合的に提供します。

## アーキテクチャ図
```
┌─────────────────────────────────────────────────────────┐
│                  Presentation Layer                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Controller  │ │ Request DTO │ │ Response DTO    │    │
│  │             │ │             │ │                 │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │ ↕ HTTP/JWT
┌─────────────────────┴───────────────────────────────────┐
│                 Security Layer                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Auth Filter │ │ JWT Service │ │ User Context    │    │
│  │             │ │             │ │                 │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│                Application Layer                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Service     │ │ Service DTO │ │ Mapper          │    │
│  │             │ │             │ │                 │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│                   Domain Layer                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Model       │ │ Repository  │ │ Domain Service  │    │
│  │ (User,Todo, │ │ Interface   │ │                 │    │
│  │ Event,Note) │ │             │ │                 │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────┐
│               Infrastructure Layer                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│  │ Entity      │ │ Repository  │ │ Security Config │    │
│  │ (JPA)       │ │ Impl        │ │                 │    │
│  └─────────────┘ └─────────────┘ └─────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## レイヤー詳細

> 📁 **詳細なフォルダ構成**: [FOLDER_STRUCTURE.md](FOLDER_STRUCTURE.md) を参照

### 1. Presentation Layer
**責務**: 外部とのインターフェース
- HTTP リクエスト/レスポンス処理
- DTO による データ転送
- 入力バリデーション

### 2. Security Layer  
**責務**: 認証・認可、セキュリティ制御
- JWT トークン検証・生成
- セキュリティコンテキスト管理
- アクセス制御

### 3. Application Layer
**責務**: ビジネスロジック、ユースケース実行
- ビジネスルール実装
- トランザクション管理
- ドメインサービスの調整

### 4. Domain Layer
**責務**: 核となるビジネスルール（他の層に依存しない）
- ビジネス概念のモデリング
- ドメインルールの実装
- データアクセスの抽象化

### 5. Infrastructure Layer
**責務**: 外部システムとの連携
- データベースアクセス実装
- 外部API連携
- 技術的な設定

**主要コンポーネント**:
- `TodoEntity`, `UserEntity`, `EventEntity`, `NoteEntity`: JPA エンティティ
- `TodoRepositoryImpl`, `UserRepositoryImpl`, `EventRepositoryImpl`, `NoteRepositoryImpl`: リポジトリ実装
- `TodoJpaRepository`, `UserJpaRepository`, `EventJpaRepository`, `NoteJpaRepository`: Spring Data JPA
- `SecurityConfig`: セキュリティ設定
- Flyway マイグレーション: データベーススキーマ管理

## 依存関係のルール
1. **内側の層は外側の層に依存しない**
2. **依存の方向**: Presentation → Security → Application → Domain ← Infrastructure
3. **Domain層は最も独立性が高い**
4. **Security層はPresentation層とApplication層の間で認証・認可を担当**

## データフロー
```
HTTP Request → Security Filter → Controller → Service → Repository Interface
       ↓              ↓               ↓           ↓              ↓
   JWT Token → Authentication → Response DTO ← Business Logic ← Repository Impl → Database
```

## セキュリティアーキテクチャ

### 認証フロー
```
1. ユーザー登録/ログイン → AuthenticationController
2. パスワード検証 → AuthenticationService  
3. JWT生成 → JwtService
4. トークン返却 → クライアント
```

### 認可フロー
```
1. APIリクエスト + JWT → JwtAuthenticationFilter
2. トークン検証 → JwtService
3. ユーザー情報設定 → SecurityContext
4. アクセス制御 → 各Service (所有者チェック)
   - TodoService: TODO所有者チェック
   - EventService: イベント所有者チェック
   - NoteService: ノート所有者チェック
   - AnalyticsService: ユーザーデータのみ集計
```

## 設計原則
1. **依存性逆転の原則**: ドメイン層がインフラ層の詳細に依存しない
2. **単一責任の原則**: 各クラスは一つの責務のみ
3. **開放閉鎖の原則**: 拡張に開いていて、変更に閉じている

## テスト戦略

### テストピラミッド
```
        ┌─────────────────┐
        │ E2E/Integration │ ← 少数・高価値
        │      Tests      │
        ├─────────────────┤
        │  Integration    │ ← 中程度・API/DB
        │     Tests       │
        ├─────────────────┤
        │   Unit Tests    │ ← 多数・高速・安価
        │                 │
        └─────────────────┘
```

### テスト分類

#### 1. 単体テスト (Unit Tests)
- **対象**: Service層のビジネスロジック
- **ツール**: JUnit 5 + Mockito
- **カバレッジ目標**: 90%以上
- **例**: `TodoServiceTest`, `AuthenticationServiceTest`

#### 2. 統合テスト (Integration Tests)
- **対象**: エンドツーエンドのAPI動作
- **ツール**: SpringBootTest + MockMvc + H2
- **テスト内容**:
  - 認証・認可フロー
  - TODO/カレンダー/ノート CRUD操作とアクセス制御
  - HTTPステータスコード検証
  - 分析機能のデータ集計検証
- **例**: `AuthenticationIntegrationTest`, `TodoIntegrationTest`, `CalendarIntegrationTest`, `NoteIntegrationTest`

#### 3. セキュリティテスト
- **対象**: 認証・認可機能
- **テスト内容**:
  - JWT トークン検証
  - アクセス制御 (403 Forbidden)
  - 認証なしアクセス (401 Unauthorized)

#### 4. データベーステスト
- **対象**: Repository層のデータアクセス
- **ツール**: @DataJpaTest + H2
- **テスト内容**: CRUD操作、クエリ検証

### テスト環境設定
- **データベース**: H2 In-Memory (テスト専用)
- **設定ファイル**: `application-test.yml`
- **マイグレーション**: 専用テスト用Flywayスクリプト
- **並列実行**: 各テストクラスが独立して実行可能

## 今後の拡張予定
1. **キャッシュ**: Redis, Spring Cache（分析データのキャッシュ）
2. **メッセージング**: RabbitMQ, Spring AMQP（リマインダー通知）
3. **監視**: Spring Boot Actuator, Micrometer
4. **検索**: Elasticsearch（全文検索）
5. **API仕様**: OpenAPI/Swagger
6. **ファイルストレージ**: S3互換ストレージ（ノート添付ファイル）
7. **バッチ処理**: Spring Batch（定期タスク生成、統計集計）