-- V2 advanced tables (7 tables) per 数据库设计.txt

CREATE TABLE knowledge_base (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    name                VARCHAR(200)  NOT NULL,
    description         VARCHAR(1000),
    industry            VARCHAR(100),
    rag_platform        VARCHAR(50)   NOT NULL DEFAULT 'BAILIAN',
    external_index_id   VARCHAR(200),
    embedding_model     VARCHAR(100),
    embedding_provider  VARCHAR(50),
    retrieval_config    VARCHAR(4096),
    process_config      VARCHAR(4096),
    doc_count           INT           NOT NULL DEFAULT 0,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    visibility          VARCHAR(20)   NOT NULL DEFAULT 'PRIVATE',
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_kb_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE knowledge_document (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id   BIGINT        NOT NULL,
    tenant_id           BIGINT        NOT NULL,
    title               VARCHAR(500)  NOT NULL,
    content             LONGTEXT,
    file_path           VARCHAR(500),
    file_type           VARCHAR(20),
    file_size           BIGINT,
    source_type         VARCHAR(50)   NOT NULL DEFAULT 'MANUAL',
    sync_status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    external_doc_id     VARCHAR(200),
    chunk_count         INT           NOT NULL DEFAULT 0,
    quality_score       DECIMAL(3,1),
    doc_version         VARCHAR(50),
    authority_level     VARCHAR(20)   NOT NULL DEFAULT 'OFFICIAL',
    valid_until         DATE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_kd_kb FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base (id)
);

CREATE TABLE routing_rule (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(200)  NOT NULL,
    priority            INT           NOT NULL DEFAULT 100,
    condition_type      VARCHAR(50)   NOT NULL,
    condition_config    VARCHAR(4096) NOT NULL,
    target_tier         VARCHAR(20)   NOT NULL DEFAULT 'STANDARD',
    target_model        VARCHAR(100),
    enabled             SMALLINT      NOT NULL DEFAULT 1,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE routing_prototype (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id            BIGINT        NOT NULL,
    group_name          VARCHAR(100)  NOT NULL,
    target_tier         VARCHAR(20)   NOT NULL DEFAULT 'STANDARD',
    prototype_text      VARCHAR(500)  NOT NULL,
    embedding_model     VARCHAR(100),
    embedding_vector    BLOB,
    enabled             SMALLINT      NOT NULL DEFAULT 1,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rp_group ON routing_prototype (group_id, enabled);

CREATE TABLE semantic_cache (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    prompt_hash         VARCHAR(64)   NOT NULL,
    prompt_text         LONGTEXT,
    model               VARCHAR(100)  NOT NULL,
    response            LONGTEXT          NOT NULL,
    prompt_tokens       INT           NOT NULL DEFAULT 0,
    completion_tokens   INT           NOT NULL DEFAULT 0,
    hit_count           INT           NOT NULL DEFAULT 0,
    expires_at          TIMESTAMP,
    last_hit_at         TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sc_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX idx_sc_tenant_hash_model ON semantic_cache (tenant_id, prompt_hash, model);

CREATE TABLE topup (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    tenant_id           BIGINT        NOT NULL,
    amount              BIGINT        NOT NULL,
    money               DECIMAL(10,2) NOT NULL DEFAULT 0,
    trade_no            VARCHAR(255)  NOT NULL UNIQUE,
    payment_method      VARCHAR(50)   NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_topup_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_topup_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE missed_query (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT        NOT NULL,
    api_key_id          BIGINT,
    query               LONGTEXT          NOT NULL,
    knowledge_base_id   BIGINT,
    top_similarity      DECIMAL(5,4),
    scene               VARCHAR(100),
    resolved            SMALLINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mq_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);