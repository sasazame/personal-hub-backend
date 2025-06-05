# フォルダ構成とその設計意図

## 概要
本プロジェクトは **ヘキサゴナルアーキテクチャ（ポート&アダプター）** に基づいて設計されています。各フォルダは明確な責務を持ち、依存関係の方向性が制御されています。

## 全体構造

```
src/main/java/com/zametech/todoapp/
├── TodoAppApplication.java          # アプリケーションエントリーポイント
├── common/                         # 共通コンポーネント
│   ├── config/                     # 設定クラス（Security等）
│   ├── exception/                  # グローバル例外ハンドリング
│   ├── util/                       # 共通ユーティリティ（現在空）
│   └── validation/                 # カスタムバリデーション
├── domain/                         # ドメイン層（依存関係の中心）
│   ├── model/                      # ドメインモデル・エンティティ
│   └── repository/                 # リポジトリインターフェース
├── application/                    # アプリケーション層
│   ├── dto/                        # アプリケーション層DTO（現在空）
│   └── service/                    # ビジネスロジック・ユースケース
├── infrastructure/                 # インフラ層
│   ├── persistence/                # データアクセス実装
│   │   ├── entity/                 # JPAエンティティ
│   │   └── repository/             # リポジトリ実装
│   └── security/                   # セキュリティ実装
└── presentation/                   # プレゼンテーション層
    ├── controller/                 # REST APIコントローラー
    ├── dto/                        # API用DTO
    │   ├── request/                # リクエストDTO
    │   └── response/               # レスポンスDTO
    └── mapper/                     # DTO変換ロジック（現在空）
```

## 各層の詳細

### 1. Common Layer (`common/`)
**責務**: プロジェクト全体で使用される共通コンポーネント

| フォルダ | 用途 | 現在の状況 | 例 |
|---------|------|-----------|---|
| `config/` | アプリケーション設定 | ✅ 実装済み | SecurityConfig |
| `exception/` | グローバル例外ハンドリング | ✅ 実装済み | GlobalExceptionHandler |
| `util/` | 共通ユーティリティ | 📦 空フォルダ | DateUtils, StringUtils等 |
| `validation/` | カスタムバリデーション | ✅ 実装済み | StrongPasswordValidator |

### 2. Domain Layer (`domain/`)
**責務**: ビジネスルールの中核、他の層に依存しない

| フォルダ | 用途 | 現在の状況 | 例 |
|---------|------|-----------|---|
| `model/` | ドメインモデル | ✅ 実装済み | Todo, User, Enum類 |
| `repository/` | データアクセス抽象化 | ✅ 実装済み | TodoRepository, UserRepository |

**設計原則**:
- 他の層への依存は一切持たない
- Pure Java（フレームワーク非依存）
- ビジネスルールのみを表現

### 3. Application Layer (`application/`)
**責務**: ユースケースの実行、ビジネスロジックの調整

| フォルダ | 用途 | 現在の状況 | 将来の使用例 |
|---------|------|-----------|------------|
| `service/` | ビジネスロジック | ✅ 実装済み | TodoService, UserService, EventService, NoteService |
| `dto/` | アプリケーション層専用DTO | 📦 空フォルダ | 複合データ転送オブジェクト |

#### `application/dto/` の将来的な使用例

```java
// 親子TODO関係の管理
public record TodoWithChildrenDto(
    Todo parentTodo,
    List<Todo> childTodos,
    TodoStatistics statistics
) {}

// ビジネスロジック用の統計情報
public record TodoStatistics(
    int totalTasks,
    int completedTasks,
    double completionRate,
    LocalDate estimatedCompletion
) {}

// 複数エンティティを組み合わせたDTO
public record UserActivitySummaryDto(
    User user,
    List<Todo> recentTodos,
    List<Event> upcomingEvents,
    List<Note> recentNotes,
    UserProductivityMetrics metrics
) {}

// 統合ダッシュボード用DTO
public record DashboardDto(
    TodoSummary todoSummary,
    EventSummary eventSummary,
    NoteSummary noteSummary,
    List<ActivityTrend> trends
) {}
```

### 4. Infrastructure Layer (`infrastructure/`)
**責務**: 外部システムとの連携、技術的な実装詳細

#### `persistence/` - データアクセス
| フォルダ | 用途 | 現在の状況 |
|---------|------|-----------|
| `entity/` | JPAエンティティ | ✅ 実装済み |
| `repository/` | リポジトリ実装 | ✅ 実装済み |

#### `security/` - セキュリティ実装
| ファイル | 用途 | 現在の状況 |
|---------|------|-----------|
| `JwtService` | JWT処理 | ✅ 実装済み |
| `JwtAuthenticationFilter` | 認証フィルター | ✅ 実装済み |
| `CustomUserDetailsService` | ユーザー詳細サービス | ✅ 実装済み |

### 5. Presentation Layer (`presentation/`)
**責務**: 外部インターフェース、HTTP通信

| フォルダ | 用途 | 現在の状況 | 将来の使用例 |
|---------|------|-----------|------------|
| `controller/` | REST APIエンドポイント | ✅ 実装済み | - |
| `dto/request/` | APIリクエストDTO | ✅ 実装済み | - |
| `dto/response/` | APIレスポンスDTO | ✅ 実装済み | - |
| `mapper/` | DTO変換ロジック | 📦 空フォルダ | 複雑な変換処理の分離 |

#### `presentation/mapper/` の将来的な使用例

```java
@Component
public class TodoMapper {
    
    public TodoWithChildrenResponse toWithChildrenResponse(TodoWithChildrenDto dto) {
        return TodoWithChildrenResponse.builder()
            .parent(TodoResponse.from(dto.parentTodo()))
            .children(dto.childTodos().stream()
                .map(TodoResponse::from)
                .toList())
            .statistics(toStatisticsResponse(dto.statistics()))
            .build();
    }
    
    public List<TodoSummaryResponse> toSummaryResponseList(
            List<Todo> todos, 
            Map<Long, List<Todo>> childrenMap) {
        // 複雑な変換ロジック
        return todos.stream()
            .map(todo -> createSummaryWithChildren(todo, childrenMap))
            .toList();
    }
}

@Component
public class DashboardMapper {
    
    public DashboardResponse toDashboardResponse(DashboardDto dto) {
        return DashboardResponse.builder()
            .todoSummary(toTodoSummaryResponse(dto.todoSummary()))
            .eventSummary(toEventSummaryResponse(dto.eventSummary()))
            .noteSummary(toNoteSummaryResponse(dto.noteSummary()))
            .trends(dto.trends().stream()
                .map(this::toActivityTrendResponse)
                .toList())
            .build();
    }
}
```

## 空フォルダの設計思想

### なぜ空フォルダを保持するか？

1. **アーキテクチャの完整性**: 設計思想を明確に示す
2. **将来の拡張性**: 機能追加時の配置場所が明確
3. **チーム開発**: 新規参加者が構造を理解しやすい
4. **一貫性**: IDEでのフォルダ構造が統一される

### 空フォルダが活用される場面

#### 1. 複雑なビジネスロジックの追加時
- 親子TODO管理
- プロジェクト進捗計算
- ユーザーアクティビティ分析

#### 2. パフォーマンス最適化時
- N+1問題の解決
- バッチ処理用DTO
- キャッシュ用データ構造

#### 3. API の複雑化時
- 複数エンティティの組み合わせ
- レポート機能
- 一括操作API

## ファイル配置のルール

### 新しいクラスを追加する際の判断基準

| 追加するクラス | 配置場所 | 判断基準 |
|---------------|----------|----------|
| API用DTO | `presentation/dto/` | 外部との通信に使用 |
| ビジネスロジック用DTO | `application/dto/` | 複数エンティティの組み合わせ |
| ドメインモデル | `domain/model/` | ビジネス概念を表現（Event, Note等） |
| 変換ロジック | `presentation/mapper/` | 複雑なDTO変換 |
| ユーティリティ | `common/util/` | 横断的な処理 |
| 分析ロジック | `application/service/` | AnalyticsService等 |

### コーディング規約

#### 1. 命名規則
```java
// Request DTO
public record CreateTodoRequest(...) {}

// Response DTO  
public record TodoResponse(...) {}

// Application層 DTO
public record TodoWithChildrenDto(...) {}

// Mapper
@Component
public class TodoMapper {}
```

#### 2. 依存関係のルール
```
presentation → application → domain ← infrastructure
     ↓              ↓
   common ←――――――――――――――――――――――――――――――――→
```

- `domain` は他の層に依存しない
- `common` はどの層からも参照可能
- 上位層から下位層への依存のみ許可

## 将来の拡張例

### シナリオ1: 親子TODO管理機能の本格実装

```java
// application/dto/
public record TodoHierarchyDto(
    Todo root,
    Map<Long, List<Todo>> childrenMap,
    HierarchyStatistics statistics
) {}

// presentation/mapper/
@Component
public class TodoHierarchyMapper {
    public TodoHierarchyResponse toResponse(TodoHierarchyDto dto) {
        // 複雑な階層構造の変換
    }
}
```

### シナリオ2: レポート機能の追加

```java
// application/dto/
public record UserProductivityReportDto(
    User user,
    Period period,
    List<Todo> completedTodos,
    List<Event> attendedEvents,
    List<Note> createdNotes,
    ProductivityMetrics metrics
) {}

// common/util/
@Component
public class DateRangeUtils {
    public static List<LocalDate> generateDateRange(Period period) {
        // 日付範囲の生成ロジック
    }
}
```

### シナリオ3: カレンダー統合機能

```java
// application/dto/
public record CalendarViewDto(
    LocalDate startDate,
    LocalDate endDate,
    List<Event> events,
    List<Todo> dueTodos,
    Map<LocalDate, DailySummary> dailySummaries
) {}

// presentation/mapper/
@Component
public class CalendarMapper {
    public CalendarViewResponse toCalendarViewResponse(CalendarViewDto dto) {
        // カレンダービューへの変換
    }
}
```

## まとめ

このフォルダ構成は、現在のシンプルな要件から将来の複雑な要件まで対応できる柔軟性を持っています。空フォルダは「将来への投資」として位置づけられ、プロジェクトの成長に合わせて自然に活用されることを想定しています。

**重要**: 現在は空でも、機能拡張時に適切な場所にコードを配置することで、保守性の高いアプリケーションを維持できます。