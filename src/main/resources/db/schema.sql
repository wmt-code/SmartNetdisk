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
