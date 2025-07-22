ALTER TABLE files ADD COLUMN is_dir BOOLEAN DEFAULT FALSE;
CREATE INDEX index_is_dir ON files(is_dir);