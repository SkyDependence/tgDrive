-- 新增“访客功能开关”配置：控制访客账户是否可登录、登录页是否展示访客凭证
-- 默认开启（true），保持与历史行为一致
INSERT INTO settings (key, value, description)
SELECT 'allow_visitor', 'true', '是否开放访客登录，true为开放，false为关闭'
WHERE NOT EXISTS (
    SELECT 1 FROM settings WHERE key = 'allow_visitor'
);
