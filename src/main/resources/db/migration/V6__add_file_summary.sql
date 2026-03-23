-- V6: 文件AI摘要
ALTER TABLE file_info ADD COLUMN IF NOT EXISTS ai_summary TEXT;
COMMENT ON COLUMN file_info.ai_summary IS 'AI生成的文件摘要';
