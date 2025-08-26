-- 创建上传会话表
CREATE TABLE IF NOT EXISTS upload_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    identifier TEXT NOT NULL UNIQUE,
    filename TEXT NOT NULL,
    total_size BIGINT NOT NULL,
    total_chunks INTEGER NOT NULL,
    chunk_size INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    status TEXT DEFAULT 'uploading',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    telegram_file_id TEXT,
    file_identifier TEXT
);

-- 创建分块记录表
CREATE TABLE IF NOT EXISTS file_chunks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    chunk_number INTEGER NOT NULL,
    chunk_id TEXT NOT NULL,
    chunk_size INTEGER NOT NULL,
    status TEXT DEFAULT 'completed',
    created_at INTEGER NOT NULL,
    FOREIGN KEY (session_id) REFERENCES upload_sessions(id) ON DELETE CASCADE
);

-- 创建索引提高查询性能
CREATE INDEX IF NOT EXISTS idx_upload_sessions_identifier ON upload_sessions(identifier);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_user_id ON upload_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_status ON upload_sessions(status);
CREATE INDEX IF NOT EXISTS idx_file_chunks_session ON file_chunks(session_id);
CREATE INDEX IF NOT EXISTS idx_file_chunks_number ON file_chunks(session_id, chunk_number);
