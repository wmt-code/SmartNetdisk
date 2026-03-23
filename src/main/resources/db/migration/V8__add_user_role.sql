-- V8: 用户角色字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'user';
COMMENT ON COLUMN sys_user.role IS '角色: admin/user';

-- 将 admin 用户设为管理员
UPDATE sys_user SET role = 'admin' WHERE username = 'admin';
