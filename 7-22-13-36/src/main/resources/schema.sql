CREATE TABLE IF NOT EXISTS files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL,
    download_url TEXT NOT NULL,
    upload_time INTEGER NOT NULL,
    size TEXT NOT NULL,
    full_size TEXT,
    file_id TEXT NOT NULL,
    webdav_path TEXT,
    is_dir BOOLEAN DEFAULT FALSE,
    user_id INTEGER REFERENCES users(id),
    is_public BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    token TEXT NOT NULL,
    target INTEGER NOT NULL,
    url INTEGER,
    pass TEXT
);

CREATE TABLE IF NOT EXISTS users (
     id INTEGER PRIMARY KEY AUTOINCREMENT,         -- 用户ID，主键，自增
     username TEXT NOT NULL UNIQUE,               -- 用户名，唯一
     password TEXT NOT NULL,                     -- 加密的用户密码
     role TEXT NOT NULL,                       -- 角色，只允许 admin、visitor
     reserved_1 TEXT DEFAULT NULL,             -- 保留字段1，便于以后扩展
     reserved_2 TEXT DEFAULT NULL,             -- 保留字段2，便于以后扩展
     reserved_3 TEXT DEFAULT NULL              -- 保留字段3，便于以后扩展
);

-- 插入初始管理员账户，防止重复插入
INSERT INTO users (username, password, role)
SELECT 'admin', 'e10adc3949ba59abbe56e057f20f883e', 'admin'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

INSERT INTO users (username, password, role)
SELECT 'visitor', '96e79218965eb72c92a549dd5a330112', 'visitor'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'visitor'
);

