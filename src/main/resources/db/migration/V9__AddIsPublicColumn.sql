-- 添加is_public字段到files表
ALTER TABLE files ADD COLUMN is_public BOOLEAN DEFAULT FALSE;
ALTER TABLE files ADD COLUMN user_id INTEGER;
CREATE INDEX idx_is_public ON files(is_public);
CREATE INDEX idx_user_id ON files(user_id);