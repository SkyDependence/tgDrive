-- 添加前端上传文件的默认 WebDAV 路径设置
INSERT INTO settings (key, value, description) 
VALUES ('default_upload_path', '/uploads/', '前端上传文件的默认WebDAV路径');
