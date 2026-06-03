-- Fix admin seed password to match admin123 (BCrypt)
UPDATE `user`
SET password_hash = '$2a$10$p4Y8X3DEtrtQ14o4pBrWd.SNgiKYGMgDYwYcJufDnsYgUtqYjfPB6'
WHERE username = 'admin';
