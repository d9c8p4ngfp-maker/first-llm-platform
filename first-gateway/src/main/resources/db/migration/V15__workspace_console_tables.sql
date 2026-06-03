CREATE TABLE skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    icon VARCHAR(50),
    prompt_template_id BIGINT,
    suggested_model VARCHAR(100),
    model_params VARCHAR(4096),
    enabled SMALLINT NOT NULL DEFAULT 1,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    usage_count INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE skill_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    binding_type VARCHAR(20) NOT NULL,
    binding_id BIGINT NOT NULL,
    config VARCHAR(4096),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mcp_server (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    server_type VARCHAR(50),
    transport VARCHAR(20) NOT NULL DEFAULT 'SSE',
    endpoint VARCHAR(500),
    command VARCHAR(500),
    env_config VARCHAR(4096),
    tools VARCHAR(4096),
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    enabled SMALLINT NOT NULL DEFAULT 1,
    last_test_at TIMESTAMP,
    last_test_result VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE user_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    nickname VARCHAR(100),
    mbti VARCHAR(10),
    mbti_label VARCHAR(50),
    zodiac VARCHAR(20),
    primary_tag VARCHAR(100),
    ai_summary VARCHAR(4000),
    ai_tags VARCHAR(4096),
    memory_count INT NOT NULL DEFAULT 0,
    profile_enabled SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_user_profile_user ON user_profile (user_id);

CREATE TABLE user_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    conversation_id BIGINT,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    category VARCHAR(30) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    importance SMALLINT NOT NULL DEFAULT 3,
    schedule_date DATE,
    schedule_time VARCHAR(10),
    numeric_value DECIMAL(15,2),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_memory_user_status ON user_memory (user_id, status);

CREATE TABLE prompt_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prompt_template_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_prompt_fav ON prompt_favorite (user_id, prompt_template_id);

CREATE TABLE pipeline_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    user_id BIGINT,
    model_id VARCHAR(100),
    model_params VARCHAR(4096),
    prompt_template_id BIGINT,
    prompt_text VARCHAR(8000),
    enabled SMALLINT NOT NULL DEFAULT 1,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_pipeline_key_scope_user ON pipeline_config (config_key, scope, user_id);

ALTER TABLE prompt_template ADD COLUMN user_id BIGINT;

INSERT INTO pipeline_config (config_key, scope, model_id, prompt_text, description, enabled) VALUES
('memory.extraction', 'SYSTEM', 'deepseek-chat', 'Extract structured memories from conversation.', 'Memory extraction pipeline', 1),
('memory.synthesis', 'SYSTEM', 'deepseek-chat', 'Synthesize user profile from memories.', 'Profile synthesis pipeline', 1),
('chat.system_prompt_base', 'SYSTEM', NULL, 'You are a helpful AI assistant.', 'Base system prompt', 1);