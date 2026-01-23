-- ===========================================
-- V3: 为文件夹表添加删除时间字段，支持回收站功能
-- ===========================================

-- 添加 delete_time 字段到 folder 表
ALTER TABLE folder ADD COLUMN IF NOT EXISTS delete_time TIMESTAMP;

COMMENT ON COLUMN folder.delete_time IS '删除时间（进入回收站的时间）';
