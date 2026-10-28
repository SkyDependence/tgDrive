-- 补充关键查询字段缺失的索引，提升下载/删除/鉴权/分页查询性能

-- file_id 是下载/删除/鉴权最频繁的查询条件，原无索引导致全表扫描
CREATE INDEX IF NOT EXISTS idx_file_id ON files(file_id);

-- upload_time 用于文件列表 ORDER BY 排序，原无索引导致 filesort
CREATE INDEX IF NOT EXISTS idx_upload_time ON files(upload_time);

-- users.email 用于注册校验和登录查询，原无索引导致全表扫描
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
