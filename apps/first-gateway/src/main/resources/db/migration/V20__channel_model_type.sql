ALTER TABLE channel_model ADD COLUMN model_type VARCHAR(20) NOT NULL DEFAULT 'CHAT';
UPDATE channel_model SET model_type = 'EMBEDDING' WHERE model_name LIKE '%embedding%';