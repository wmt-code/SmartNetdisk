-- V5: 通知系统
CREATE TABLE IF NOT EXISTS notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(500),
    is_read SMALLINT NOT NULL DEFAULT 0,
    related_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_user ON notification(user_id, is_read, create_time DESC);

COMMENT ON TABLE notification IS '用户通知表';
COMMENT ON COLUMN notification.type IS '通知类型: upload/share/ai/system';
COMMENT ON COLUMN notification.is_read IS '是否已读: 0-未读, 1-已读';
COMMENT ON COLUMN notification.related_id IS '关联ID(文件ID/分享ID等)';
