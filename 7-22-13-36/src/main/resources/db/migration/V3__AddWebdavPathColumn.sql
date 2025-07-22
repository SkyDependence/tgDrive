ALTER TABLE files ADD COLUMN webdav_path TEXT DEFAULT '';
UPDATE files SET webdav_path = '' WHERE webdav_path IS NULL;
CREATE INDEX idx_webdav_path ON files(webdav_path);