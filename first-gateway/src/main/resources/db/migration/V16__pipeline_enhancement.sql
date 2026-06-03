-- user_profile enhancements for synthesis
ALTER TABLE user_profile ADD COLUMN version INT DEFAULT 0;
ALTER TABLE user_profile ADD COLUMN last_synthesis_count INT DEFAULT 0;
ALTER TABLE user_profile ADD COLUMN synthesis_status VARCHAR(20) DEFAULT 'IDLE';
ALTER TABLE user_profile ADD COLUMN ai_system_prompt VARCHAR(4000);

-- user_memory reminder flag
ALTER TABLE user_memory ADD COLUMN reminded SMALLINT DEFAULT 0;

-- knowledge_document index error
ALTER TABLE knowledge_document ADD COLUMN index_error VARCHAR(500);

-- console_user_preference add language
ALTER TABLE console_user_preference ADD COLUMN language VARCHAR(20) DEFAULT 'zh-CN';

-- model routing priority in console_user_preference
ALTER TABLE console_user_preference ADD COLUMN routing_priority VARCHAR(500);
