-- V19: Repair platform admin password if V18 was applied with placeholder hash

SET @admin_bcrypt = '$2a$10$p4Y8X3DEtrtQ14o4pBrWd.SNgiKYGMgDYwYcJufDnsYgUtqYjfPB6';

UPDATE `user`
SET password_hash = @admin_bcrypt
WHERE username IN ('admin', 'platform_admin')
  AND (password_hash LIKE '%REPLACE%' OR password_hash NOT LIKE '$2a$%');

UPDATE user_tenant_rel utr
INNER JOIN `user` u ON u.id = utr.user_id
SET utr.role = 'PLATFORM_ADMIN'
WHERE u.username IN ('admin', 'platform_admin');
