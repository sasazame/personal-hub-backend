# データベース設計書

## 概要
PostgreSQL 16を使用したPersonal Hub統合アプリケーションのデータベース設計
TODO管理、カレンダー、ノート機能を統合的に管理

## ERダイアグラム
```
┌─────────────────────────────────────────────┐
│                   users                     │
├─────────────────────────────────────────────┤
│ id (BIGSERIAL) PK                          │
│ username (VARCHAR(50)) NOT NULL UNIQUE     │
│ email (VARCHAR(255)) NOT NULL UNIQUE       │
│ password (VARCHAR(255)) NOT NULL           │
│ enabled (BOOLEAN) NOT NULL DEFAULT TRUE    │
│ created_at (TIMESTAMP) NOT NULL            │
│ updated_at (TIMESTAMP) NOT NULL            │
└─────────────────────────────────────────────┘
         │              │              │
         │ 1:N          │ 1:N          │ 1:N
         ▼              ▼              ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│    todos    │ │   events    │ │    notes    │
├─────────────┤ ├─────────────┤ ├─────────────┤
│ id          │ │ id          │ │ id          │
│ user_id FK  │ │ user_id FK  │ │ user_id FK  │
│ parent_id FK│ │ title       │ │ title       │
│ title       │ │ description │ │ content     │
│ description │ │ start_date  │ │ created_at  │
│ status      │ │ end_date    │ │ updated_at  │
│ priority    │ │ location    │ └─────────────┘
│ due_date    │ │ is_all_day  │        │
│ created_at  │ │ reminder    │        │ 1:N
│ updated_at  │ │ created_at  │        ▼
└─────────────┘ │ updated_at  │ ┌─────────────┐
       │ 1:N    └─────────────┘ │ note_tags   │
       ▼                        ├─────────────┤
 (self-reference)               │ note_id FK  │
                               │ tag_name    │
                               └─────────────┘
```

## テーブル定義

### users テーブル
| カラム名 | データ型 | 制約 | 説明 |
|---------|----------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自動採番ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | ユーザー名 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | メールアドレス |
| password | VARCHAR(255) | NOT NULL | ハッシュ化パスワード |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | アカウント有効状態 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 作成日時 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 更新日時 |

### todos テーブル
| カラム名 | データ型 | 制約 | 説明 |
|---------|----------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自動採番ID |
| user_id | BIGINT | NOT NULL, FK → users.id | 所有者ユーザーID |
| parent_id | BIGINT | NULL, FK → todos.id | 親タスクID |
| title | VARCHAR(255) | NOT NULL | TODOタイトル |
| description | TEXT | NULL | 詳細説明 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'TODO' | ステータス |
| priority | VARCHAR(10) | NOT NULL, DEFAULT 'MEDIUM' | 優先度 |
| due_date | DATE | NULL | 期限日 |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 更新日時 |

### events テーブル
| カラム名 | データ型 | 制約 | 説明 |
|---------|----------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自動採番ID |
| user_id | BIGINT | NOT NULL, FK → users.id | 所有者ユーザーID |
| title | VARCHAR(255) | NOT NULL | イベントタイトル |
| description | TEXT | NULL | 詳細説明 |
| start_date | TIMESTAMPTZ | NOT NULL | 開始日時 |
| end_date | TIMESTAMPTZ | NOT NULL | 終了日時 |
| location | VARCHAR(255) | NULL | 場所 |
| is_all_day | BOOLEAN | NOT NULL, DEFAULT FALSE | 終日フラグ |
| reminder | INTEGER | NULL | リマインダー（分） |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 更新日時 |

### notes テーブル
| カラム名 | データ型 | 制約 | 説明 |
|---------|----------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自動採番ID |
| user_id | BIGINT | NOT NULL, FK → users.id | 所有者ユーザーID |
| title | VARCHAR(255) | NOT NULL | ノートタイトル |
| content | TEXT | NOT NULL | ノート内容 |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 作成日時 |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 更新日時 |

### note_tags テーブル
| カラム名 | データ型 | 制約 | 説明 |
|---------|----------|------|------|
| note_id | BIGINT | NOT NULL, FK → notes.id | ノートID |
| tag_name | VARCHAR(50) | NOT NULL | タグ名 |
| PRIMARY KEY | (note_id, tag_name) | 複合主キー | - |

## 制約

### 外部キー制約
```sql
-- TODO → ユーザー関連付け
ALTER TABLE todos ADD CONSTRAINT fk_todos_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- TODO → 親TODO関連付け
ALTER TABLE todos ADD CONSTRAINT fk_todos_parent_id 
    FOREIGN KEY (parent_id) REFERENCES todos(id) ON DELETE CASCADE;

-- イベント → ユーザー関連付け
ALTER TABLE events ADD CONSTRAINT fk_events_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ノート → ユーザー関連付け
ALTER TABLE notes ADD CONSTRAINT fk_notes_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ノートタグ → ノート関連付け
ALTER TABLE note_tags ADD CONSTRAINT fk_note_tags_note_id 
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE;
```

### CHECK制約
```sql
-- ステータス制約
ALTER TABLE todos ADD CONSTRAINT chk_status
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'));

-- 優先度制約  
ALTER TABLE todos ADD CONSTRAINT chk_priority
    CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'));
```

### インデックス
```sql
-- users テーブル
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(enabled);

-- todos テーブル
CREATE INDEX idx_todos_user_id ON todos(user_id);
CREATE INDEX idx_todos_parent_id ON todos(parent_id);
CREATE INDEX idx_todos_user_status ON todos(user_id, status);
CREATE INDEX idx_todos_status ON todos(status);
CREATE INDEX idx_todos_due_date ON todos(due_date);

-- events テーブル
CREATE INDEX idx_events_user_id ON events(user_id);
CREATE INDEX idx_events_start_date ON events(start_date);
CREATE INDEX idx_events_end_date ON events(end_date);
CREATE INDEX idx_events_user_date_range ON events(user_id, start_date, end_date);

-- notes テーブル
CREATE INDEX idx_notes_user_id ON notes(user_id);
CREATE INDEX idx_notes_updated_at ON notes(updated_at);
CREATE INDEX idx_notes_user_updated ON notes(user_id, updated_at DESC);

-- note_tags テーブル
CREATE INDEX idx_note_tags_tag_name ON note_tags(tag_name);
```

## トリガー

### 更新日時自動更新
```sql
-- 更新日時を自動更新する関数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- トリガーの作成
CREATE TRIGGER update_todos_updated_at BEFORE UPDATE
    ON todos FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_events_updated_at BEFORE UPDATE
    ON events FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notes_updated_at BEFORE UPDATE
    ON notes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE
    ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

## マイグレーション

### Flyway設定
- **ファイル配置**: `src/main/resources/db/migration/`
- **命名規則**: `V{version}__{description}.sql`
- **マイグレーション履歴**:
  - `V1__create_todo_table.sql`: TODOテーブル作成
  - `V2__create_user_table.sql`: ユーザーテーブル作成
  - `V3__add_user_id_to_todos.sql`: TODO-ユーザー関連付け
  - `V4__update_user_table_to_username.sql`: ユーザー名カラム追加
  - `V5__add_parent_id_to_todos.sql`: 親子TODO関係
  - `V6__create_events_table.sql`: イベントテーブル作成（予定）
  - `V7__create_notes_table.sql`: ノートテーブル作成（予定）
  - `V8__create_note_tags_table.sql`: ノートタグテーブル作成（予定）

### 設定
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
```

## セットアップ手順

### 1. データベース作成
```bash
sudo -u postgres psql -c "CREATE DATABASE personalhub;"
sudo -u postgres psql -c "CREATE USER personalhub WITH ENCRYPTED PASSWORD 'personalhub';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE personalhub TO personalhub;"
sudo -u postgres psql -c "ALTER DATABASE personalhub OWNER TO personalhub;"
```

### 2. 接続設定
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/personalhub
    username: personalhub
    password: personalhub
    driver-class-name: org.postgresql.Driver
```

## パフォーマンス考慮事項

### 接続プール（HikariCP）
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### クエリ最適化
1. **ユーザー別TODO検索**: `idx_todos_user_id` インデックス使用
2. **ユーザー・ステータス組み合わせ**: `idx_todos_user_status` 複合インデックス使用
3. **メール検索**: `idx_users_email` インデックス使用
4. **期限日検索**: `idx_todos_due_date` インデックス使用
5. **イベント日付範囲検索**: `idx_events_user_date_range` 複合インデックス使用
6. **ノート更新順検索**: `idx_notes_user_updated` 複合インデックス使用
7. **タグ検索**: `idx_note_tags_tag_name` インデックス使用

### セキュリティ考慮事項
1. **パスワードハッシュ化**: BCrypt使用（コスト12）
2. **カスケード削除**: ユーザー削除時の関連データ自動削除
3. **データ分離**: ユーザー別のデータアクセス制御（TODO、イベント、ノート）
4. **SQLインジェクション対策**: プリペアドステートメント使用

## 現在の実装状況

### 実装済み機能
1. ✅ **users**: ユーザー管理（認証用）
2. ✅ **todos**: TODO管理（親子関係、所有者制御付き）
3. ✅ **外部キー制約**: データ整合性保証
4. ✅ **インデックス**: パフォーマンス最適化

### 実装予定機能
1. 🔄 **events**: カレンダーイベント管理
2. 🔄 **notes**: ノート管理
3. 🔄 **note_tags**: タグ機能

### 将来の拡張予定
1. **attachments**: ファイル添付（ノート、TODO）
2. **notifications**: 通知管理（リマインダー、期限通知）
3. **recurring_tasks**: 定期タスク設定
4. **user_preferences**: ユーザー設定（タイムゾーン、言語等）
5. **activity_logs**: アクティビティログ（監査ログ）

### 拡張時の設計方針
- **外部キー制約**: 参照整合性保証
- **論理削除**: deleted_at カラム追加
- **監査ログ**: created_by, updated_by カラム追加

## バックアップ・復旧
```bash
# バックアップ
pg_dump -h localhost -U personalhub -d personalhub > backup.sql

# 復旧
psql -h localhost -U personalhub -d personalhub < backup.sql
```