ALTER TABLE channel ADD COLUMN tenant_id BIGINT NULL;
ALTER TABLE channel ADD COLUMN user_id BIGINT NULL;
UPDATE channel SET tenant_id = 1, user_id = 1 WHERE tenant_id IS NULL;
CREATE INDEX idx_channel_user_deleted ON channel (user_id, deleted);