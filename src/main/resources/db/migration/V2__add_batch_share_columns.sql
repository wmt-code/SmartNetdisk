-- 为 share 表添加批量分享相关字段
-- 请在 PostgreSQL 数据库中执行此脚本

-- !! 重要：允许 file_id 为 NULL（用于文件夹/批量分享）
ALTER TABLE share ALTER COLUMN file_id DROP NOT NULL;

-- 添加分享类型字段（0-单文件，1-目录，2-批量）
ALTER TABLE share ADD COLUMN IF NOT EXISTS share_type INTEGER DEFAULT 0;

-- 添加文件夹ID（目录分享时使用）
ALTER TABLE share ADD COLUMN IF NOT EXISTS folder_id BIGINT DEFAULT NULL;

-- 添加分享标题（批量分享时使用）
ALTER TABLE share ADD COLUMN IF NOT EXISTS share_title VARCHAR(200) DEFAULT NULL;

-- 添加总大小（字节）
ALTER TABLE share ADD COLUMN IF NOT EXISTS total_size BIGINT DEFAULT 0;

-- 添加文件数量
ALTER TABLE share ADD COLUMN IF NOT EXISTS file_count INTEGER DEFAULT 1;

-- 更新现有记录的 share_type 为单文件类型
UPDATE share SET share_type = 0 WHERE share_type IS NULL;

-- 如果 share_item 表不存在，则创建
CREATE TABLE IF NOT EXISTS share_item (
    id BIGSERIAL PRIMARY KEY,
    share_id BIGINT NOT NULL,
    item_type INTEGER NOT NULL DEFAULT 0,  -- 0-文件, 1-文件夹
    file_id BIGINT,
    folder_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_share_item_share FOREIGN KEY (share_id) REFERENCES share(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_share_item_share_id ON share_item(share_id);

COMMENT ON COLUMN share.share_type IS '分享类型: 0-单文件, 1-目录, 2-批量';
COMMENT ON COLUMN share.folder_id IS '文件夹ID（目录分享时使用）';
COMMENT ON COLUMN share.share_title IS '分享标题';
COMMENT ON COLUMN share.total_size IS '分享总大小（字节）';
COMMENT ON COLUMN share.file_count IS '包含的文件数量';
