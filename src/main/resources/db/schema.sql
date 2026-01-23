-- ===========================================
-- SmartNetdisk 数据库初始化脚本 (PostgreSQL)
-- 版本: 1.0.0
-- 创建日期: 2026-01-21
-- ===========================================

-- 启用 pgvector 扩展（需要先安装 pgvector 扩展）
CREATE EXTENSION IF NOT EXISTS vector;

-- ===========================================
-- 1. 用户表 (sys_user)
-- ===========================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    avatar VARCHAR(255),
    used_space BIGINT NOT NULL DEFAULT 0,
    total_space BIGINT NOT NULL DEFAULT 10737418240,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_user_username ON sys_user(username);
CREATE INDEX IF NOT EXISTS idx_user_email ON sys_user(email);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.password IS '密码（BCrypt加密）';
COMMENT ON COLUMN sys_user.avatar IS '头像URL';
COMMENT ON COLUMN sys_user.used_space IS '已用空间（字节）';
COMMENT ON COLUMN sys_user.total_space IS '总空间（默认10GB）';
COMMENT ON COLUMN sys_user.status IS '状态: 0-禁用, 1-正常';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除: 0-未删除, 1-已删除';

-- ===========================================
-- 2. 文件信息表 (file_info)
-- ===========================================
CREATE TABLE IF NOT EXISTS file_info (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    folder_id BIGINT DEFAULT 0,
    file_name VARCHAR(255) NOT NULL,
    file_md5 VARCHAR(32) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(20),
    file_ext VARCHAR(20),
    mime_type VARCHAR(100),
    storage_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    is_vectorized SMALLINT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    delete_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_file_user_folder ON file_info(user_id, folder_id);
CREATE INDEX IF NOT EXISTS idx_file_md5 ON file_info(file_md5);
CREATE INDEX IF NOT EXISTS idx_file_user_deleted ON file_info(user_id, deleted);

COMMENT ON TABLE file_info IS '文件信息表';
COMMENT ON COLUMN file_info.id IS '文件ID';
COMMENT ON COLUMN file_info.user_id IS '所属用户ID';
COMMENT ON COLUMN file_info.folder_id IS '所属文件夹ID, 0表示根目录';
COMMENT ON COLUMN file_info.file_name IS '文件名';
COMMENT ON COLUMN file_info.file_md5 IS '文件MD5';
COMMENT ON COLUMN file_info.file_size IS '文件大小（字节）';
COMMENT ON COLUMN file_info.file_type IS '文件类型: image/video/audio/document/other';
COMMENT ON COLUMN file_info.file_ext IS '文件扩展名';
COMMENT ON COLUMN file_info.mime_type IS 'MIME类型';
COMMENT ON COLUMN file_info.storage_path IS '存储路径（MinIO对象路径）';
COMMENT ON COLUMN file_info.thumbnail_path IS '缩略图路径';
COMMENT ON COLUMN file_info.is_vectorized IS '是否已向量化: 0-否, 1-是';
COMMENT ON COLUMN file_info.status IS '状态: 0-上传中, 1-正常, 2-转码中';
COMMENT ON COLUMN file_info.delete_time IS '删除时间（进入回收站的时间）';

-- ===========================================
-- 3. 文件夹表 (folder)
-- ===========================================
CREATE TABLE IF NOT EXISTS folder (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    folder_name VARCHAR(100) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_folder_user_parent ON folder(user_id, parent_id);

COMMENT ON TABLE folder IS '文件夹表';
COMMENT ON COLUMN folder.id IS '文件夹ID';
COMMENT ON COLUMN folder.user_id IS '所属用户ID';
COMMENT ON COLUMN folder.parent_id IS '父文件夹ID, 0表示根目录';
COMMENT ON COLUMN folder.folder_name IS '文件夹名称';

-- ===========================================
-- 4. 分片上传记录表 (file_chunk)
-- ===========================================
CREATE TABLE IF NOT EXISTS file_chunk (
    id BIGSERIAL PRIMARY KEY,
    file_md5 VARCHAR(32) NOT NULL,
    chunk_index INT NOT NULL,
    total_chunks INT NOT NULL,
    chunk_size BIGINT NOT NULL,
    chunk_path VARCHAR(500) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (file_md5, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_chunk_file_md5 ON file_chunk(file_md5);

COMMENT ON TABLE file_chunk IS '文件分片上传记录表';
COMMENT ON COLUMN file_chunk.id IS '分片ID';
COMMENT ON COLUMN file_chunk.file_md5 IS '文件MD5';
COMMENT ON COLUMN file_chunk.chunk_index IS '分片索引（从0开始）';
COMMENT ON COLUMN file_chunk.total_chunks IS '总分片数';
COMMENT ON COLUMN file_chunk.chunk_size IS '分片大小（字节）';
COMMENT ON COLUMN file_chunk.chunk_path IS '分片存储路径';
COMMENT ON COLUMN file_chunk.status IS '状态: 0-上传中, 1-已完成';

-- ===========================================
-- 5. 分享表 (share)
-- ===========================================
CREATE TABLE IF NOT EXISTS share (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    share_code VARCHAR(16) NOT NULL UNIQUE,
    password VARCHAR(10),
    expire_time TIMESTAMP,
    view_count INT NOT NULL DEFAULT 0,
    download_count INT NOT NULL DEFAULT 0,
    max_view_count INT,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_share_user_id ON share(user_id);
CREATE INDEX IF NOT EXISTS idx_share_file_id ON share(file_id);
CREATE INDEX IF NOT EXISTS idx_share_code ON share(share_code);

COMMENT ON TABLE share IS '文件分享表';
COMMENT ON COLUMN share.id IS '分享ID';
COMMENT ON COLUMN share.user_id IS '分享者用户ID';
COMMENT ON COLUMN share.file_id IS '被分享的文件ID';
COMMENT ON COLUMN share.share_code IS '分享短码（唯一）';
COMMENT ON COLUMN share.password IS '提取密码（可选）';
COMMENT ON COLUMN share.expire_time IS '过期时间, NULL表示永久';
COMMENT ON COLUMN share.view_count IS '访问次数';
COMMENT ON COLUMN share.download_count IS '下载次数';
COMMENT ON COLUMN share.max_view_count IS '最大访问次数限制';
COMMENT ON COLUMN share.status IS '状态: 0-已取消, 1-有效, 2-已过期';

-- ===========================================
-- 6. 文档向量化记录表 (vector_document) - 使用 pgvector
-- ===========================================
CREATE TABLE IF NOT EXISTS vector_document (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1024),  -- 向量维度根据 BAAI/bge-large-zh-v1.5 模型调整
    token_count INT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vector_file_id ON vector_document(file_id);
CREATE INDEX IF NOT EXISTS idx_vector_user_id ON vector_document(user_id);

-- 创建向量索引（IVFFlat索引，适合中等规模数据）
-- 注意：需要先插入一些数据后才能创建 IVFFlat 索引
-- 如果表中数据量小于 lists 参数，可能会报错
-- 建议在有足够数据后再创建此索引
-- CREATE INDEX idx_vector_embedding ON vector_document
--     USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 或者使用 HNSW 索引（效果更好，但占用更多内存）
-- CREATE INDEX idx_vector_embedding ON vector_document
--     USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE vector_document IS '文档向量化记录表';
COMMENT ON COLUMN vector_document.id IS '向量文档ID';
COMMENT ON COLUMN vector_document.file_id IS '关联的文件ID';
COMMENT ON COLUMN vector_document.user_id IS '所属用户ID';
COMMENT ON COLUMN vector_document.chunk_index IS '分块索引（从0开始）';
COMMENT ON COLUMN vector_document.content IS '分块文本内容';
COMMENT ON COLUMN vector_document.embedding IS '文本向量（pgvector类型）';
COMMENT ON COLUMN vector_document.token_count IS 'Token数量';

-- ===========================================
-- 完成提示
-- ===========================================
-- 数据库初始化完成！
--
-- 注意事项：
-- 1. 需要先安装 pgvector 扩展：apt install postgresql-16-pgvector 或从源码编译
-- 2. 向量索引建议在有一定数据量后再创建
-- 3. 生产环境需要添加外键约束
-- 4. 根据实际 Embedding 模型调整 vector 维度

-- ===========================================
-- 数据库迁移脚本 v2.0.0 - 目录分享功能
-- 创建日期: 2026-01-22
-- ===========================================

-- ===========================================
-- 1. 扩展 share 表以支持目录和批量分享
-- ===========================================

-- 1.1 修改 file_id 为可空（向后兼容）
ALTER TABLE share ALTER COLUMN file_id DROP NOT NULL;

-- 1.2 添加分享类型字段
ALTER TABLE share ADD COLUMN IF NOT EXISTS share_type SMALLINT NOT NULL DEFAULT 0;
COMMENT ON COLUMN share.share_type IS '分享类型: 0-单文件分享, 1-目录分享, 2-批量分享';

-- 1.3 添加文件夹ID字段
ALTER TABLE share ADD COLUMN IF NOT EXISTS folder_id BIGINT;
COMMENT ON COLUMN share.folder_id IS '被分享的文件夹ID（当share_type=1时使用）';

-- 1.4 添加分享标题字段
ALTER TABLE share ADD COLUMN IF NOT EXISTS share_title VARCHAR(100);
COMMENT ON COLUMN share.share_title IS '分享标题（批量分享时的自定义标题）';

-- 1.5 添加总大小字段
ALTER TABLE share ADD COLUMN IF NOT EXISTS total_size BIGINT;
COMMENT ON COLUMN share.total_size IS '分享总大小（字节）';

-- 1.6 添加文件数量统计字段
ALTER TABLE share ADD COLUMN IF NOT EXISTS file_count INT DEFAULT 0;
COMMENT ON COLUMN share.file_count IS '包含的文件数量';

-- 1.7 添加索引
CREATE INDEX IF NOT EXISTS idx_share_folder_id ON share(folder_id);
CREATE INDEX IF NOT EXISTS idx_share_type ON share(share_type);

-- ===========================================
-- 2. 创建分享项明细表 share_item
-- ===========================================
CREATE TABLE IF NOT EXISTS share_item (
    id BIGSERIAL PRIMARY KEY,
    share_id BIGINT NOT NULL,
    item_type SMALLINT NOT NULL,
    file_id BIGINT,
    folder_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 添加外键约束（可选，取决于生产环境策略）
-- ALTER TABLE share_item ADD CONSTRAINT fk_share_item_share
--     FOREIGN KEY (share_id) REFERENCES share(id) ON DELETE CASCADE;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_share_item_share_id ON share_item(share_id);
CREATE INDEX IF NOT EXISTS idx_share_item_file_id ON share_item(file_id);
CREATE INDEX IF NOT EXISTS idx_share_item_folder_id ON share_item(folder_id);

-- 添加注释
COMMENT ON TABLE share_item IS '分享项明细表（支持批量分享）';
COMMENT ON COLUMN share_item.id IS '分享项ID';
COMMENT ON COLUMN share_item.share_id IS '关联的分享ID';
COMMENT ON COLUMN share_item.item_type IS '项类型: 0-文件, 1-文件夹';
COMMENT ON COLUMN share_item.file_id IS '文件ID（当item_type=0时）';
COMMENT ON COLUMN share_item.folder_id IS '文件夹ID（当item_type=1时）';

-- ===========================================
-- 3. 数据迁移：现有分享记录迁移
-- ===========================================

-- 3.1 更新现有分享记录的 share_type 为 0（单文件分享）
-- 并填充 file_count 和 total_size 字段
UPDATE share
SET share_type = 0,
    file_count = 1,
    total_size = (
        SELECT COALESCE(file_size, 0)
        FROM file_info
        WHERE id = share.file_id
    )
WHERE share_type = 0 AND file_id IS NOT NULL;

-- 3.2 为现有分享创建 share_item 记录（确保数据一致性）
-- 使用 ON CONFLICT DO NOTHING 防止重复插入
INSERT INTO share_item (share_id, item_type, file_id)
SELECT id, 0, file_id
FROM share
WHERE share_type = 0 AND file_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- ===========================================
-- 4. 验证迁移结果
-- ===========================================
-- 查询各类型分享的统计信息
-- SELECT
--     COUNT(*) as total_shares,
--     SUM(CASE WHEN share_type = 0 THEN 1 ELSE 0 END) as single_file_shares,
--     SUM(CASE WHEN share_type = 1 THEN 1 ELSE 0 END) as folder_shares,
--     SUM(CASE WHEN share_type = 2 THEN 1 ELSE 0 END) as batch_shares
-- FROM share;

-- ===========================================
-- 迁移脚本 v2.0.0 完成！
-- ===========================================

-- ===========================================
-- 数据库优化脚本 - 文件查询性能优化
-- 创建日期: 2026-01-23
-- ===========================================

-- 1. 优化文件表复合索引（包含deleted字段，避免回表）
DROP INDEX IF EXISTS idx_file_user_folder;
CREATE INDEX idx_file_user_folder_deleted ON file_info(user_id, folder_id, deleted);

-- 2. 优化文件夹表复合索引（包含deleted字段）
DROP INDEX IF EXISTS idx_folder_user_parent;
CREATE INDEX idx_folder_user_parent_deleted ON folder(user_id, parent_id, deleted);

-- 3. 添加覆盖索引优化统计查询
CREATE INDEX IF NOT EXISTS idx_file_count_opt ON file_info(user_id, folder_id, deleted) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_folder_count_opt ON folder(user_id, parent_id, deleted) WHERE deleted = 0;
