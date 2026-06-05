ALTER TABLE knowledge_document ADD COLUMN source_url VARCHAR(2000) DEFAULT NULL;
ALTER TABLE knowledge_document ADD COLUMN crawled_at TIMESTAMP DEFAULT NULL;
ALTER TABLE knowledge_document ADD COLUMN word_count INT DEFAULT 0;
ALTER TABLE knowledge_document ADD COLUMN auto_summary TEXT DEFAULT NULL;
