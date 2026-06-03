CREATE TABLE billing_record (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    type        VARCHAR(20) NOT NULL,
    amount      BIGINT NOT NULL,
    ref_log_id  BIGINT,
    remark      VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_billing_record_tenant ON billing_record (tenant_id, created_at);