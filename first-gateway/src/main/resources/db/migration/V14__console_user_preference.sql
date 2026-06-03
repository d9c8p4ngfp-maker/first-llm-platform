CREATE TABLE console_user_preference (
    user_id           BIGINT        NOT NULL PRIMARY KEY,
    default_model     VARCHAR(100),
    preferences_json  VARCHAR(4096),
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);