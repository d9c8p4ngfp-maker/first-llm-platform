-- V17: Workspace Console tables — foreign keys, indexes, constraints
-- Gen6 — D1 + D2: add FK constraints + fix pipeline_config UNIQUE

-- ========================================
-- 1. skill — foreign keys
-- ========================================
ALTER TABLE skill
  ADD CONSTRAINT fk_skill_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
  ADD CONSTRAINT fk_skill_user    FOREIGN KEY (user_id)   REFERENCES `user`(id);

-- prompt_template_id FK kept commented (column nullable)
-- ALTER TABLE skill ADD CONSTRAINT fk_skill_prompt FOREIGN KEY (prompt_template_id) REFERENCES prompt_template(id);

-- ========================================
-- 2. skill_binding — foreign key + indexes
-- ========================================
ALTER TABLE skill_binding
  ADD CONSTRAINT fk_sb_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE;

CREATE INDEX idx_skill_binding_skill   ON skill_binding (skill_id);
CREATE INDEX idx_skill_binding_target  ON skill_binding (binding_type, binding_id);

-- ========================================
-- 3. mcp_server — foreign keys
-- ========================================
ALTER TABLE mcp_server
  ADD CONSTRAINT fk_mcp_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
  ADD CONSTRAINT fk_mcp_user   FOREIGN KEY (user_id)   REFERENCES `user`(id);

-- ========================================
-- 4. user_profile — foreign keys
-- ========================================
ALTER TABLE user_profile
  ADD CONSTRAINT fk_up_user   FOREIGN KEY (user_id)   REFERENCES `user`(id),
  ADD CONSTRAINT fk_up_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

-- ========================================
-- 5. user_memory — foreign keys + query indexes + CHECK
-- ========================================
ALTER TABLE user_memory
  ADD CONSTRAINT fk_um_user   FOREIGN KEY (user_id)   REFERENCES `user`(id),
  ADD CONSTRAINT fk_um_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);

CREATE INDEX idx_user_memory_schedule  ON user_memory (user_id, schedule_date, status, category);
CREATE INDEX idx_user_memory_category  ON user_memory (user_id, category, status, created_at DESC);
CREATE INDEX idx_user_memory_numeric   ON user_memory (user_id, status, numeric_value, created_at DESC);

ALTER TABLE user_memory
  ADD CONSTRAINT chk_um_importance CHECK (importance BETWEEN 1 AND 5);

ALTER TABLE user_memory MODIFY schedule_time TIME NULL;

-- ========================================
-- 6. prompt_favorite — foreign keys
-- ========================================
ALTER TABLE prompt_favorite
  ADD CONSTRAINT fk_pf_user   FOREIGN KEY (user_id)            REFERENCES `user`(id),
  ADD CONSTRAINT fk_pf_prompt FOREIGN KEY (prompt_template_id)  REFERENCES prompt_template(id);

-- ========================================
-- 7. pipeline_config UNIQUE fix (D2)
--    MySQL UNIQUE treats NULL as distinct, allowing duplicate SYSTEM rows.
--    Fix: force SYSTEM rows to user_id=0 (sentinel).
-- ========================================
UPDATE pipeline_config SET user_id = 0 WHERE scope = 'SYSTEM' AND user_id IS NULL;

ALTER TABLE pipeline_config MODIFY user_id BIGINT NOT NULL DEFAULT 0;

DROP INDEX uk_pipeline_key_scope_user ON pipeline_config;
CREATE UNIQUE INDEX uk_pipeline_key_scope_user ON pipeline_config (config_key, scope, user_id);

-- pipeline_config FK kept commented (column nullable)
-- ALTER TABLE pipeline_config ADD CONSTRAINT fk_pc_prompt FOREIGN KEY (prompt_template_id) REFERENCES prompt_template(id);

-- ========================================
-- 11. user_memory 补充 message_id 和 valid_until
-- ========================================
ALTER TABLE user_memory
  ADD COLUMN message_id BIGINT NULL AFTER conversation_id,
  ADD COLUMN valid_until TIMESTAMP NULL AFTER schedule_time;

-- ========================================
-- 12. user_profile 补充字段
-- ========================================
ALTER TABLE user_profile
  ADD COLUMN ai_system_prompt TEXT NULL AFTER ai_tags,
  ADD COLUMN ai_personality VARCHAR(4096) NULL AFTER ai_system_prompt,
  ADD COLUMN last_analyzed_at TIMESTAMP NULL AFTER ai_personality,
  ADD COLUMN synthesis_status VARCHAR(20) NOT NULL DEFAULT 'IDLE' AFTER last_analyzed_at;

-- ========================================
-- 13. token_usage_log 补充 user_id
-- ========================================
ALTER TABLE token_usage_log
  ADD COLUMN user_id BIGINT NULL AFTER tenant_id;

CREATE INDEX idx_tul_user_created ON token_usage_log (user_id, created_at);

UPDATE token_usage_log tul
JOIN api_key ak ON tul.api_key_id = ak.id
SET tul.user_id = ak.user_id
WHERE tul.user_id IS NULL AND ak.user_id IS NOT NULL;

-- ========================================
-- 14. billing_record 唯一约束
-- ========================================
ALTER TABLE billing_record
  ADD UNIQUE INDEX uk_billing_ref (tenant_id, ref_log_id);

-- ========================================
-- 15. JSON 字段从 VARCHAR 改为 JSON/TEXT
-- ========================================
ALTER TABLE skill MODIFY model_params JSON NULL;
ALTER TABLE mcp_server MODIFY env_config TEXT NULL;
ALTER TABLE mcp_server MODIFY tools JSON NULL;
ALTER TABLE pipeline_config MODIFY model_params JSON NULL;
ALTER TABLE pipeline_config MODIFY prompt_text TEXT NULL;
