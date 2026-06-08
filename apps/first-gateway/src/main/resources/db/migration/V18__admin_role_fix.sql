-- V18: Platform admin role fix + close registration
-- Promotes seed user `admin` (password: admin123) and optional platform_admin alias

-- BCrypt hash for password "admin123" (same as V4 seed)
SET @admin_bcrypt = '$2a$10$p4Y8X3DEtrtQ14o4pBrWd.SNgiKYGMgDYwYcJufDnsYgUtqYjfPB6';

-- 1. Ensure platform_admin account exists (same password as admin123)
INSERT INTO `user` (username, email, password_hash, status, created_at, updated_at)
SELECT 'platform_admin', 'admin@first-gateway.local', @admin_bcrypt, 'ACTIVE', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'platform_admin');

SET @platform_admin_id = (SELECT id FROM `user` WHERE username = 'platform_admin');
SET @def_tenant = (SELECT id FROM tenant ORDER BY id LIMIT 1);

INSERT INTO user_tenant_rel (user_id, tenant_id, role, joined_at)
VALUES (@platform_admin_id, @def_tenant, 'PLATFORM_ADMIN', NOW())
ON DUPLICATE KEY UPDATE role = 'PLATFORM_ADMIN';

-- 2. Promote existing seed admin to PLATFORM_ADMIN (preserves admin123 login)
UPDATE user_tenant_rel
SET role = 'PLATFORM_ADMIN'
WHERE user_id = (SELECT id FROM `user` WHERE username = 'admin' LIMIT 1);

-- 3. Demote remaining OWNER roles to MEMBER
UPDATE user_tenant_rel SET role = 'MEMBER' WHERE role = 'OWNER';

-- 4. Close public registration
INSERT INTO system_config (config_key, config_value, updated_at)
VALUES ('register_enabled', 'false', NOW())
ON DUPLICATE KEY UPDATE config_value = 'false';
