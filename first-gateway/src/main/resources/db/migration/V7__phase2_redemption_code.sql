CREATE TABLE redemption_code (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(32) NOT NULL UNIQUE,
    amount     BIGINT NOT NULL,
    used_by    BIGINT,
    used_at    TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rc_user FOREIGN KEY (used_by) REFERENCES `user` (id)
);