-- ユーザーIDをBIGINTからUUIDに変更するマイグレーション

-- Step 1: 一時的なUUIDカラムを追加
ALTER TABLE users ADD COLUMN new_id UUID DEFAULT gen_random_uuid();

-- Step 2: 新しいUUIDでnew_idを更新（既存レコード用）
UPDATE users SET new_id = gen_random_uuid() WHERE new_id IS NULL;

-- Step 3: 外部キーを持つテーブルにも一時的なUUIDカラムを追加
ALTER TABLE todos ADD COLUMN new_user_id UUID;
ALTER TABLE events ADD COLUMN new_user_id UUID;
ALTER TABLE notes ADD COLUMN new_user_id UUID;
ALTER TABLE calendar_sync_settings ADD COLUMN new_user_id UUID;

-- Step 4: 外部キーのマッピングを更新
UPDATE todos SET new_user_id = (SELECT new_id FROM users WHERE users.id = todos.user_id);
UPDATE events SET new_user_id = (SELECT new_id FROM users WHERE users.id = events.user_id);
UPDATE notes SET new_user_id = (SELECT new_id FROM users WHERE users.id = notes.user_id);
UPDATE calendar_sync_settings SET new_user_id = (SELECT new_id FROM users WHERE users.id = calendar_sync_settings.user_id);

-- Step 5: 古い外部キー制約を削除
ALTER TABLE todos DROP CONSTRAINT IF EXISTS fk_todos_user_id;
ALTER TABLE events DROP CONSTRAINT IF EXISTS fk_events_user_id;
ALTER TABLE notes DROP CONSTRAINT IF EXISTS fk_notes_user_id;
ALTER TABLE calendar_sync_settings DROP CONSTRAINT IF EXISTS calendar_sync_settings_user_id_fkey;

-- Step 6: 古いカラムを削除
ALTER TABLE todos DROP COLUMN user_id;
ALTER TABLE events DROP COLUMN user_id;
ALTER TABLE notes DROP COLUMN user_id;
ALTER TABLE calendar_sync_settings DROP COLUMN user_id;
ALTER TABLE users DROP COLUMN id;

-- Step 7: 新しいカラムをリネームして主キーに設定
ALTER TABLE users RENAME COLUMN new_id TO id;
ALTER TABLE todos RENAME COLUMN new_user_id TO user_id;
ALTER TABLE events RENAME COLUMN new_user_id TO user_id;
ALTER TABLE notes RENAME COLUMN new_user_id TO user_id;
ALTER TABLE calendar_sync_settings RENAME COLUMN new_user_id TO user_id;

-- Step 8: 新しい主キー制約を追加
ALTER TABLE users ADD PRIMARY KEY (id);

-- Step 9: NOT NULL制約を追加
ALTER TABLE todos ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE events ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE notes ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE calendar_sync_settings ALTER COLUMN user_id SET NOT NULL;

-- Step 10: 新しい外部キー制約を追加
ALTER TABLE todos ADD CONSTRAINT fk_todos_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    
ALTER TABLE events ADD CONSTRAINT fk_events_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    
ALTER TABLE notes ADD CONSTRAINT fk_notes_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE calendar_sync_settings ADD CONSTRAINT fk_calendar_sync_settings_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Step 11: インデックスを再作成
DROP INDEX IF EXISTS idx_todos_user_id;
DROP INDEX IF EXISTS idx_events_user_id;
DROP INDEX IF EXISTS idx_notes_user_id;
DROP INDEX IF EXISTS idx_calendar_sync_settings_user_id;

CREATE INDEX idx_todos_user_id ON todos(user_id);
CREATE INDEX idx_events_user_id ON events(user_id);
CREATE INDEX idx_notes_user_id ON notes(user_id);
CREATE INDEX idx_calendar_sync_settings_user_id ON calendar_sync_settings(user_id);

-- Step 12: calendar_sync_settingsのユニーク制約を再作成
DROP INDEX IF EXISTS uk_calendar_sync_settings_user_calendar;
ALTER TABLE calendar_sync_settings ADD CONSTRAINT uk_calendar_sync_settings_user_calendar
    UNIQUE (user_id, google_calendar_id);