-- 创建上传任务表，用于支持断点续传
CREATE TABLE IF NOT EXISTS upload_tasks (
    id VARCHAR(64) PRIMARY KEY,              -- 任务ID (fileHash_userId)
    user_id BIGINT NOT NULL,                 -- 用户ID
    file_name VARCHAR(500) NOT NULL,         -- 文件名
    file_size BIGINT NOT NULL,               -- 文件大小(字节)
    file_hash VARCHAR(64) NOT NULL,          -- 文件SHA256哈希
    chunk_size INT NOT NULL DEFAULT 10485760,-- 分块大小(默认10MB)
    total_chunks INT NOT NULL,               -- 总分块数
    uploaded_chunks TEXT,                     -- JSON数组记录已上传分块索引 [0,1,2...]
    chunk_file_ids TEXT,                      -- JSON对象记录分块fileId {"0":"fileId1","1":"fileId2"}
    status VARCHAR(20) NOT NULL,             -- 任务状态: pending/uploading/paused/completed/failed
    error_message TEXT,                       -- 错误信息(如果失败)
    final_file_id VARCHAR(255),              -- 最终文件ID(完成后)
    created_at BIGINT,                        -- 创建时间(UNIX时间戳)
    updated_at BIGINT,                        -- 更新时间(UNIX时间戳)
    expires_at BIGINT                         -- 过期时间(UNIX时间戳)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_uploader_tasks_user_id ON upload_tasks (user_id);
CREATE INDEX IF NOT EXISTS idx_file_hash ON upload_tasks (file_hash);
CREATE INDEX IF NOT EXISTS idx_status ON upload_tasks (status);
CREATE INDEX IF NOT EXISTS idx_expires_at ON upload_tasks (expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_hash_user ON upload_tasks (file_hash, user_id);