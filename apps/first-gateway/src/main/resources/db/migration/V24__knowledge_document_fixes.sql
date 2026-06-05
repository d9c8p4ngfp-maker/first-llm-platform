-- V24 fix knowledge_document table design issues:
--  1. Add composite index for listDocuments query
--  2. Add tenant_id index for tenant isolation queries
--  3. Add foreign key constraint to tenant table
--  4. Add CHECK constraint: content must be NULL for non-MANUAL sources

ALTER TABLE knowledge_document ADD INDEX idx_kd_kbid_deleted (knowledge_base_id, deleted);
ALTER TABLE knowledge_document ADD INDEX idx_kd_tenant (tenant_id);
ALTER TABLE knowledge_document ADD CONSTRAINT fk_kd_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id);
ALTER TABLE knowledge_document ADD CONSTRAINT ck_kd_content_non_manual_null
    CHECK (source_type = 'MANUAL' OR content IS NULL);
