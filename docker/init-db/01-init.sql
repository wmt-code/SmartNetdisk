-- SmartNetdisk 本地开发环境数据库初始化
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 用户表
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

-- 文件信息表
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

CREATE INDEX IF NOT EXISTS idx_file_md5 ON file_info(file_md5);
CREATE INDEX IF NOT EXISTS idx_file_user_deleted ON file_info(user_id, deleted);
CREATE INDEX IF NOT EXISTS idx_file_user_folder_deleted ON file_info(user_id, folder_id, deleted);
CREATE INDEX IF NOT EXISTS idx_file_count_opt ON file_info(user_id, folder_id, deleted) WHERE deleted = 0;

-- 文件夹表
CREATE TABLE IF NOT EXISTS folder (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    folder_name VARCHAR(100) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    delete_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_folder_user_parent_deleted ON folder(user_id, parent_id, deleted);
CREATE INDEX IF NOT EXISTS idx_folder_count_opt ON folder(user_id, parent_id, deleted) WHERE deleted = 0;

-- 分片上传记录表
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

-- 分享表
CREATE TABLE IF NOT EXISTS share (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id BIGINT,
    share_code VARCHAR(16) NOT NULL UNIQUE,
    password VARCHAR(10),
    expire_time TIMESTAMP,
    view_count INT NOT NULL DEFAULT 0,
    download_count INT NOT NULL DEFAULT 0,
    max_view_count INT,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    share_type SMALLINT NOT NULL DEFAULT 0,
    folder_id BIGINT,
    share_title VARCHAR(100),
    total_size BIGINT,
    file_count INT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_share_user_id ON share(user_id);
CREATE INDEX IF NOT EXISTS idx_share_file_id ON share(file_id);
CREATE INDEX IF NOT EXISTS idx_share_code ON share(share_code);
CREATE INDEX IF NOT EXISTS idx_share_folder_id ON share(folder_id);
CREATE INDEX IF NOT EXISTS idx_share_type ON share(share_type);

-- 分享项明细表
CREATE TABLE IF NOT EXISTS share_item (
    id BIGSERIAL PRIMARY KEY,
    share_id BIGINT NOT NULL,
    item_type SMALLINT NOT NULL DEFAULT 0,
    file_id BIGINT,
    folder_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_share_item_share_id ON share_item(share_id);
CREATE INDEX IF NOT EXISTS idx_share_item_file_id ON share_item(file_id);
CREATE INDEX IF NOT EXISTS idx_share_item_folder_id ON share_item(folder_id);

-- 文档向量化记录表
CREATE TABLE IF NOT EXISTS vector_document (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1024),
    token_count INT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vector_file_id ON vector_document(file_id);
CREATE INDEX IF NOT EXISTS idx_vector_user_id ON vector_document(user_id);
