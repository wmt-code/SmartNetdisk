-- V4: 添加文件最近访问时间字段
ALTER TABLE file_info ADD COLUMN IF NOT EXISTS last_access_time TIMESTAMP;

-- 初始化为创建时间
UPDATE file_info SET last_access_time = create_time WHERE last_access_time IS NULL;

-- 添加索引用于"最近访问"查询
CREATE INDEX IF NOT EXISTS idx_file_last_access ON file_info(user_id, last_access_time DESC) WHERE deleted = 0;
