-- 创建WebDAV配置表
CREATE TABLE webdav_config (
    id BIGINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT 1,
    path_prefix VARCHAR(255) NOT NULL DEFAULT '/webdav',
    require_auth BOOLEAN NOT NULL DEFAULT 1,
    allowed_roles VARCHAR(100) NOT NULL DEFAULT 'admin',
    max_upload_size INTEGER NOT NULL DEFAULT 100,
    allow_mkdir BOOLEAN NOT NULL DEFAULT 1,
    allow_delete BOOLEAN NOT NULL DEFAULT 1,
    allow_move BOOLEAN NOT NULL DEFAULT 1,
    allow_copy BOOLEAN NOT NULL DEFAULT 1,
    description TEXT,
    create_time BIGINT NOT NULL,
    update_time BIGINT NOT NULL
);

-- 插入默认配置
INSERT INTO webdav_config (
    id, enabled, path_prefix, require_auth, allowed_roles,
    max_upload_size, allow_mkdir, allow_delete, allow_move,
    allow_copy, description, create_time, update_time
) VALUES (
    1, 1, '/webdav', 1, 'admin',
    100, 1, 1, 1,
    1, 'WebDAV服务配置', 1642694400000, 1642694400000
);