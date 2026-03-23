-- V7: AI 对话会话持久化
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL DEFAULT '新对话',
    mode VARCHAR(20) NOT NULL DEFAULT 'global',
    messages TEXT NOT NULL DEFAULT '[]',
    scoped_file_ids TEXT DEFAULT '[]',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_session_user ON chat_session(user_id, update_time DESC);

COMMENT ON TABLE chat_session IS 'AI对话会话表';
COMMENT ON COLUMN chat_session.mode IS '模式: global/scoped';
COMMENT ON COLUMN chat_session.messages IS '消息JSON数组';
COMMENT ON COLUMN chat_session.scoped_file_ids IS '指定文件ID的JSON数组';
