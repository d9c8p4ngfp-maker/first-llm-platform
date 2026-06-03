-- MVP core schema (14 tables) per 数据库设计.txt

CREATE TABLE tenant (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    type            VARCHAR(20)   NOT NULL DEFAULT 'PERSONAL',
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    max_members     INT           NOT NULL DEFAULT 1,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT      NOT NULL DEFAULT 0
);

CREATE TABLE `user` (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100)  NOT NULL UNIQUE,
    email           VARCHAR(200)  UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    phone           VARCHAR(20),
    avatar_url      VARCHAR(500),
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT      NOT NULL DEFAULT 0
);

CREATE TABLE user_tenant_rel (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    tenant_id       BIGINT        NOT NULL,
    role            VARCHAR(20)   NOT NULL DEFAULT 'MEMBER',
    joined_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_utr_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_utr_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT uk_utr_user_tenant UNIQUE (user_id, tenant_id)
);

CREATE TABLE subscription (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    plan                VARCHAR(20)   NOT NULL DEFAULT 'FREE',
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    monthly_quota       BIGINT        NOT NULL DEFAULT 0,
    daily_rate_limit    INT           NOT NULL DEFAULT 100,
    features            VARCHAR(4096),
    quota_reset_period  VARCHAR(20)   NOT NULL DEFAULT 'MONTHLY',
    last_reset_at       TIMESTAMP,
    next_reset_at       TIMESTAMP,
    started_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX idx_subscription_tenant_status ON subscription (tenant_id, status);

CREATE TABLE channel (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100)  NOT NULL,
    type                VARCHAR(50)   NOT NULL,
    provider            VARCHAR(50),
    base_url            VARCHAR(500)  NOT NULL,
    api_key_encrypted   VARCHAR(500) NOT NULL,
    priority            INT           NOT NULL DEFAULT 0,
    weight              INT           NOT NULL DEFAULT 1,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    max_rpm             INT           NOT NULL DEFAULT 0,
    used_quota          BIGINT        NOT NULL DEFAULT 0,
    config              VARCHAR(4096),
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_channel_status_type ON channel (status, type);

CREATE TABLE channel_model (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id      BIGINT        NOT NULL,
    model_name      VARCHAR(100)  NOT NULL,
    model_alias     VARCHAR(100),
    input_ratio     DECIMAL(10,4) NOT NULL DEFAULT 1.0000,
    output_ratio    DECIMAL(10,4) NOT NULL DEFAULT 1.0000,
    cache_ratio     DECIMAL(10,4) NOT NULL DEFAULT 1.0000,
    max_context     INT,
    tier            VARCHAR(20)   NOT NULL DEFAULT 'STANDARD',
    enabled         SMALLINT      NOT NULL DEFAULT 1,
    CONSTRAINT fk_cm_channel FOREIGN KEY (channel_id) REFERENCES channel (id),
    CONSTRAINT uk_cm_channel_model UNIQUE (channel_id, model_name)
);

CREATE TABLE api_key (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    user_id             BIGINT        NOT NULL,
    name                VARCHAR(100),
    key_hash            VARCHAR(255)  NOT NULL UNIQUE,
    key_prefix          VARCHAR(20)   NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    remaining_quota     BIGINT        NOT NULL DEFAULT -1,
    used_quota          BIGINT        NOT NULL DEFAULT 0,
    rate_limit          INT           NOT NULL DEFAULT -1,
    rate_config         VARCHAR(4096),
    allowed_models      VARCHAR(4096),
    security_config     VARCHAR(4096),
    expires_at          TIMESTAMP,
    last_used_at        TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_ak_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_ak_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE INDEX idx_api_key_tenant ON api_key (tenant_id, status);

CREATE TABLE quota (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT        NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    total_tokens    BIGINT        NOT NULL DEFAULT 0,
    used_tokens     BIGINT        NOT NULL DEFAULT 0,
    period_start    DATE,
    period_end      DATE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quota_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX idx_quota_tenant_type ON quota (tenant_id, type);

CREATE TABLE token_usage_log (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT,
    api_key_id          BIGINT,
    model               VARCHAR(100),
    channel_id          BIGINT,
    prompt_tokens       INT,
    completion_tokens   INT,
    total_tokens        INT,
    cost_ratio          DECIMAL(6,3),
    unit_type           VARCHAR(20)   NOT NULL DEFAULT 'TOKEN',
    cache_hit           SMALLINT      NOT NULL DEFAULT 0,
    rag_used            SMALLINT      NOT NULL DEFAULT 0,
    routing_model       VARCHAR(100),
    is_stream           SMALLINT      NOT NULL DEFAULT 0,
    latency_ms          INT,
    status              VARCHAR(20),
    error_msg           VARCHAR(500),
    request_id          VARCHAR(64),
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tul_tenant_created ON token_usage_log (tenant_id, created_at);
CREATE INDEX idx_tul_api_key_created ON token_usage_log (api_key_id, created_at);
CREATE INDEX idx_tul_request_id ON token_usage_log (request_id);

CREATE TABLE conversation (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT        NOT NULL,
    api_key_id              BIGINT,
    session_id              VARCHAR(64)   NOT NULL UNIQUE,
    summary                 LONGTEXT,
    key_entities            VARCHAR(4096),
    message_count           INT           NOT NULL DEFAULT 0,
    total_input_tokens      BIGINT        NOT NULL DEFAULT 0,
    total_output_tokens     BIGINT        NOT NULL DEFAULT 0,
    saved_tokens            BIGINT        NOT NULL DEFAULT 0,
    window_size             INT           NOT NULL DEFAULT 10,
    model                   VARCHAR(100),
    context_config          VARCHAR(4096),
    status                  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    last_message_at         TIMESTAMP,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conv_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX idx_conversation_tenant ON conversation (tenant_id, status);

CREATE TABLE conversation_message (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id     BIGINT        NOT NULL,
    role                VARCHAR(20)   NOT NULL,
    content             LONGTEXT,
    tokens              INT,
    compressed          SMALLINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cm_conv FOREIGN KEY (conversation_id) REFERENCES conversation (id)
);

CREATE INDEX idx_conv_msg_conv_created ON conversation_message (conversation_id, created_at);

CREATE TABLE prompt_template (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT,
    name                    VARCHAR(200)  NOT NULL,
    description             VARCHAR(1000),
    industry                VARCHAR(100),
    category                VARCHAR(100),
    current_version_id      BIGINT,
    visibility              VARCHAR(20)   NOT NULL DEFAULT 'PRIVATE',
    usage_count             BIGINT        NOT NULL DEFAULT 0,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT      NOT NULL DEFAULT 0
);

CREATE TABLE prompt_version (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id             BIGINT        NOT NULL,
    version                 VARCHAR(20)   NOT NULL,
    system_prompt           LONGTEXT,
    user_prompt_template    LONGTEXT,
    variables               VARCHAR(4096),
    suggested_model         VARCHAR(100),
    ab_weight               INT           NOT NULL DEFAULT 100,
    changelog               VARCHAR(1000),
    created_by              BIGINT,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pv_template FOREIGN KEY (template_id) REFERENCES prompt_template (id),
    CONSTRAINT fk_pv_user FOREIGN KEY (created_by) REFERENCES `user` (id)
);

CREATE TABLE system_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key      VARCHAR(100)  NOT NULL UNIQUE,
    config_value    LONGTEXT          NOT NULL,
    description     VARCHAR(500),
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);