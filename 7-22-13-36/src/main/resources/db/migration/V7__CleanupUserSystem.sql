-- 清理用户系统，只保留admin和visitor账户
-- 删除除admin和visitor之外的所有用户
DELETE FROM users WHERE username NOT IN ('admin', 'visitor');

-- 确保admin和visitor账户存在且密码正确
-- admin密码: 123456 (MD5: e10adc3949ba59abbe56e057f20f883e)
-- visitor密码: hello (MD5: 96e79218965eb72c92a549dd5a330112)

-- 更新或插入admin账户
INSERT OR REPLACE INTO users (id, username, password, role) 
VALUES (
    (SELECT id FROM users WHERE username = 'admin'),
    'admin', 
    'e10adc3949ba59abbe56e057f20f883e', 
    'admin'
);

-- 更新或插入visitor账户
INSERT OR REPLACE INTO users (id, username, password, role) 
VALUES (
    (SELECT id FROM users WHERE username = 'visitor'),
    'visitor', 
    '96e79218965eb72c92a549dd5a330112', 
    'visitor'
);

-- 修改role列为NOT NULL（如果还不是的话）
-- SQLite不支持直接修改列约束，所以我们通过重建表来实现
CREATE TABLE users_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL,
    reserved_1 TEXT DEFAULT NULL,
    reserved_2 TEXT DEFAULT NULL,
    reserved_3 TEXT DEFAULT NULL
);

-- 复制数据
INSERT INTO users_new SELECT * FROM users;

-- 删除旧表
DROP TABLE users;

-- 重命名新表
ALTER TABLE users_new RENAME TO users;