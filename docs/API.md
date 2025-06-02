# API仕様書

## 概要
TODO管理アプリケーションのRESTful API仕様

## ベース情報
- **ベースURL**: `http://localhost:8080/api/v1`
- **データ形式**: JSON
- **文字エンコーディング**: UTF-8
- **認証**: JWT Bearer Token（一部エンドポイントを除く）

## 共通仕様

### HTTPステータスコード
| コード | 説明 |
|--------|------|
| 200 | OK - 成功 |
| 201 | Created - 作成成功 |
| 204 | No Content - 削除成功 |
| 400 | Bad Request - リクエストエラー |
| 401 | Unauthorized - 認証が必要 |
| 403 | Forbidden - アクセス権限なし |
| 404 | Not Found - リソースが見つからない |
| 409 | Conflict - データ競合エラー |
| 500 | Internal Server Error - サーバーエラー |

### 認証ヘッダー
認証が必要なエンドポイントでは、以下のヘッダーを含める必要があります：
```
Authorization: Bearer <JWT_TOKEN>
```

### エラーレスポンス形式
```json
{
  "code": "ERROR_CODE",
  "message": "エラーメッセージ",
  "details": {
    "field": "詳細情報"
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

## エンドポイント一覧

## 🔓 認証エンドポイント（認証不要）

### 1. ユーザー登録
```
POST /api/v1/auth/register
```

**リクエストボディ**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "username": "testuser"
}
```

**レスポンス** (201 Created):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "user@example.com",
    "createdAt": "2024-01-01T09:00:00+09:00",
    "updatedAt": "2024-01-01T09:00:00+09:00"
  }
}
```

**エラーレスポンス** (409 Conflict - ユーザーが既に存在):
```json
{
  "code": "USER_ALREADY_EXISTS",
  "message": "User already exists with email: user@example.com",
  "timestamp": "2025-05-30T12:00:00Z"
}
```

### 2. ログイン
```
POST /api/v1/auth/login
```

**リクエストボディ**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**レスポンス** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "user@example.com",
    "createdAt": "2024-01-01T09:00:00+09:00",
    "updatedAt": "2024-01-01T09:00:00+09:00"
  }
}
```

**エラーレスポンス** (401 Unauthorized - 認証失敗):
```json
{
  "code": "AUTHENTICATION_FAILED",
  "message": "Invalid email or password",
  "timestamp": "2025-05-30T12:00:00Z"
}
```

### 3. 現在のユーザー情報取得
```
GET /api/v1/auth/me
Authorization: Bearer <JWT_TOKEN>
```

**レスポンス** (200 OK):
```json
{
  "id": 1,
  "username": "testuser",
  "email": "user@example.com",
  "createdAt": "2024-01-01T09:00:00+09:00",
  "updatedAt": "2024-01-01T09:00:00+09:00"
}
```

## 🔒 TODOエンドポイント（認証必須）

### 4. TODO作成
```
POST /api/v1/todos
Authorization: Bearer <JWT_TOKEN>
```

**リクエストボディ**:
```json
{
  "title": "サンプルTODO",
  "description": "詳細説明（任意）",
  "priority": "HIGH",
  "dueDate": "2024-12-31",
  "parentId": null
}
```

**レスポンス** (201 Created):
```json
{
  "id": 1,
  "title": "サンプルTODO",
  "description": "詳細説明（任意）",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2024-12-31",
  "parentId": null,
  "createdAt": "2024-01-01T09:00:00+09:00",
  "updatedAt": "2024-01-01T09:00:00+09:00"
}
```

**注意**: 作成されたTODOは認証済みユーザーに自動的に関連付けられます。

### 5. TODO取得（ID指定）
```
GET /api/v1/todos/{id}
Authorization: Bearer <JWT_TOKEN>
```

**パスパラメータ**:
- `id`: TODO ID (Long)

**レスポンス** (200 OK):
```json
{
  "id": 1,
  "title": "サンプルTODO",
  "description": "詳細説明",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2024-12-31",
  "parentId": null,
  "createdAt": "2024-01-01T09:00:00+09:00",
  "updatedAt": "2024-01-01T09:00:00+09:00"
}
```

**エラーレスポンス** (403 Forbidden - 他のユーザーのTODO):
```json
{
  "code": "ACCESS_DENIED",
  "message": "Access denied to TODO with id: 1",
  "timestamp": "2025-05-30T12:00:00Z"
}
```

### 6. TODO一覧取得
```
GET /api/v1/todos
Authorization: Bearer <JWT_TOKEN>
```

**クエリパラメータ**:
- `page`: ページ番号（デフォルト: 0）
- `size`: 1ページあたりの件数（デフォルト: 20）
- `sort`: ソート条件（デフォルト: createdAt,desc）

**レスポンス** (200 OK):
認証済みユーザーのTODOのみが返されます。
```json
{
  "content": [
    {
      "id": 1,
      "title": "サンプルTODO",
      "description": "詳細説明",
      "status": "TODO",
      "priority": "HIGH",
      "dueDate": "2024-12-31",
      "parentId": null,
      "createdAt": "2024-01-01T09:00:00+09:00",
      "updatedAt": "2024-01-01T09:00:00+09:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "orderBy": "createdAt",
      "direction": "DESC"
    }
  },
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### 7. ステータス別TODO取得
```
GET /api/v1/todos/status/{status}
Authorization: Bearer <JWT_TOKEN>
```

**パスパラメータ**:
- `status`: TODO, IN_PROGRESS, DONE

**レスポンス** (200 OK):
認証済みユーザーの指定ステータスのTODOのみが返されます。
```json
[
  {
    "id": 1,
    "title": "進行中のTODO",
    "description": "詳細説明",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "dueDate": "2024-12-31",
    "parentId": null,
    "createdAt": "2024-01-01T09:00:00+09:00",
    "updatedAt": "2024-01-01T10:00:00+09:00"
  }
]
```

### 8. TODO更新
```
PUT /api/v1/todos/{id}
Authorization: Bearer <JWT_TOKEN>
```

**パスパラメータ**:
- `id`: TODO ID (Long)

**リクエストボディ**:
```json
{
  "title": "更新されたTODO",
  "description": "更新された詳細説明",
  "status": "IN_PROGRESS",
  "priority": "MEDIUM",
  "dueDate": "2024-12-25"
}
```

**レスポンス** (200 OK):
```json
{
  "id": 1,
  "title": "更新されたTODO",
  "description": "更新された詳細説明",
  "status": "IN_PROGRESS",
  "priority": "MEDIUM",
  "dueDate": "2024-12-25",
  "parentId": null,
  "createdAt": "2024-01-01T09:00:00+09:00",
  "updatedAt": "2024-01-01T11:00:00+09:00"
}
```

**エラーレスポンス** (403 Forbidden - 他のユーザーのTODO):
```json
{
  "code": "ACCESS_DENIED",
  "message": "Access denied to update TODO with id: 1",
  "timestamp": "2025-05-30T12:00:00Z"
}
```

### 9. TODO削除
```
DELETE /api/v1/todos/{id}
Authorization: Bearer <JWT_TOKEN>
```

**パスパラメータ**:
- `id`: TODO ID (Long)

**レスポンス** (204 No Content):
レスポンスボディなし

**エラーレスポンス** (403 Forbidden - 他のユーザーのTODO):
```json
{
  "code": "ACCESS_DENIED",
  "message": "Access denied to delete TODO with id: 1",
  "timestamp": "2025-05-30T12:00:00Z"
}
```

### 10. 子タスク一覧取得
```
GET /api/v1/todos/{parentId}/children
Authorization: Bearer <JWT_TOKEN>
```

**パスパラメータ**:
- `parentId`: 親TODO ID (Long)

**レスポンス** (200 OK):
認証済みユーザーの指定された親TODOの子タスクが返されます。
```json
[
  {
    "id": 2,
    "title": "子タスク1",
    "description": "詳細説明",
    "status": "TODO",
    "priority": "MEDIUM",
    "dueDate": "2024-12-31",
    "parentId": 1,
    "createdAt": "2024-01-01T09:30:00+09:00",
    "updatedAt": "2024-01-01T09:30:00+09:00"
  }
]
```

## 🔒 ユーザー管理エンドポイント（認証必須）

### 11. ユーザー情報取得
```
GET /api/v1/users/{id}
Authorization: Bearer <JWT_TOKEN>
```

**パスパラメータ**:
- `id`: ユーザー ID (Long)

**レスポンス** (200 OK):
```json
{
  "id": 1,
  "username": "testuser",
  "email": "user@example.com",
  "createdAt": "2024-01-01T09:00:00+09:00",
  "updatedAt": "2024-01-01T09:00:00+09:00"
}
```

### 12. ユーザー情報更新
```
PUT /api/v1/users/{id}
Authorization: Bearer <JWT_TOKEN>
```

**パスパラメータ**:
- `id`: ユーザー ID (Long)

**リクエストボディ**:
```json
{
  "username": "newusername",
  "email": "newemail@example.com",
  "currentPassword": "currentPassword",
  "newPassword": "NewSecurePass123!"
}
```

**レスポンス** (200 OK):
```json
{
  "id": 1,
  "username": "newusername",
  "email": "newemail@example.com",
  "createdAt": "2024-01-01T09:00:00+09:00",
  "updatedAt": "2024-01-01T12:00:00+09:00"
}
```

### 13. パスワード変更
```
PUT /api/v1/users/{id}/password
Authorization: Bearer <JWT_TOKEN>
```

**パスパラメータ**:
- `id`: ユーザー ID (Long)

**リクエストボディ**:
```json
{
  "currentPassword": "currentPassword",
  "newPassword": "NewSecurePass123!"
}
```

**レスポンス** (204 No Content):
レスポンスボディなし

### 14. ユーザーアカウント削除
```
DELETE /api/v1/users/{id}
Authorization: Bearer <JWT_TOKEN>
```

**パスパラメータ**:
- `id`: ユーザー ID (Long)

**レスポンス** (204 No Content):
レスポンスボディなし

## データモデル

### TodoStatus (Enum)
- `TODO`: 未着手
- `IN_PROGRESS`: 進行中
- `DONE`: 完了

### TodoPriority (Enum)
- `HIGH`: 高
- `MEDIUM`: 中
- `LOW`: 低

## バリデーション

### RegisterRequest
- `email`: 必須、有効なメールアドレス形式
- `password`: 必須、強力なパスワード（8文字以上、大文字・小文字・数字・特殊文字を含む）
- `username`: 必須、3-20文字

### LoginRequest
- `email`: 必須、有効なメールアドレス形式
- `password`: 必須

### CreateTodoRequest
- `title`: 必須、最大255文字
- `description`: 任意、最大1000文字
- `priority`: 任意（デフォルト: MEDIUM）
- `dueDate`: 任意
- `parentId`: 任意、親TODO ID

### UpdateTodoRequest
- `title`: 必須、最大255文字
- `description`: 任意、最大1000文字
- `status`: 必須
- `priority`: 必須
- `dueDate`: 任意
- `parentId`: 任意、親TODO ID

### UpdateUserRequest
- `username`: 任意、3-20文字
- `email`: 任意、有効なメールアドレス形式
- `currentPassword`: 必須（パスワード変更時）
- `newPassword`: 任意、強力なパスワード

### ChangePasswordRequest
- `currentPassword`: 必須
- `newPassword`: 必須、強力なパスワード

## セキュリティ設定

### 認証・認可
- **認証方式**: JWT Bearer Token
- **トークン有効期限**: 24時間（デフォルト）
- **アクセス制御**: ユーザーは自分のTODOのみアクセス可能

### CORS設定
- **許可オリジン**: `http://localhost:3000`
- **許可メソッド**: GET, POST, PUT, DELETE, OPTIONS
- **許可ヘッダー**: Authorization, Content-Type, Accept
- **資格情報**: 許可

## 使用例

### cURLサンプル

#### 1. ユーザー登録
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "username": "testuser"
  }'
```

#### 2. ログイン
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

#### 3. TODO作成（要認証）
```bash
curl -X POST http://localhost:8080/api/v1/todos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "テストTODO",
    "description": "テスト用のTODO",
    "priority": "HIGH",
    "dueDate": "2024-12-31"
  }'
```

#### 4. TODO一覧取得（要認証）
```bash
curl -X GET "http://localhost:8080/api/v1/todos?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 5. TODO更新（要認証）
```bash
curl -X PUT http://localhost:8080/api/v1/todos/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "更新されたTODO",
    "description": "更新された説明",
    "status": "DONE",
    "priority": "LOW",
    "dueDate": "2024-12-25"
  }'
```

#### 6. 現在のユーザー情報取得（要認証）
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 7. 子タスク一覧取得（要認証）
```bash
curl -X GET http://localhost:8080/api/v1/todos/1/children \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 8. ステータス別TODO取得（要認証）
```bash
curl -X GET http://localhost:8080/api/v1/todos/status/IN_PROGRESS \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 9. パスワード変更（要認証）
```bash
curl -X PUT http://localhost:8080/api/v1/users/1/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "currentPassword": "SecurePass123!",
    "newPassword": "NewSecurePass456!"
  }'
```

## 今後の機能拡張予定
1. **検索機能**: タイトル・説明での部分一致検索
2. **カテゴリー・タグ**: TODO の分類機能
3. **一括操作**: 複数TODO の一括更新・削除
4. **ファイル添付**: TODO へのファイル添付機能
5. **通知機能**: 期限間近の TODO 通知
6. **OpenAPI**: Swagger UI での API ドキュメント