-- Initial seed data

INSERT INTO tenant (id, name, type, status, max_members)
VALUES (1, 'Admin Personal', 'PERSONAL', 'ACTIVE', 1);

INSERT INTO `user` (id, username, email, password_hash, status)
VALUES (1, 'admin', 'admin@first.local', '$2a$10$p4Y8X3DEtrtQ14o4pBrWd.SNgiKYGMgDYwYcJufDnsYgUtqYjfPB6', 'ACTIVE');

INSERT INTO user_tenant_rel (user_id, tenant_id, role)
VALUES (1, 1, 'OWNER');

INSERT INTO subscription (tenant_id, plan, status, monthly_quota, daily_rate_limit, features, quota_reset_period, started_at)
VALUES (1, 'PRO', 'ACTIVE', 5000000, -1, '{"rag":true,"prompt":true,"routing":true,"cache":true}', 'MONTHLY', CURRENT_TIMESTAMP);

INSERT INTO quota (tenant_id, type, total_tokens, used_tokens, period_start)
VALUES (1, 'SUBSCRIPTION', 5000000, 0, CURRENT_DATE);

INSERT INTO channel (id, name, type, provider, base_url, api_key_encrypted, priority, weight, status, max_rpm)
VALUES (1, 'DeepSeek Test', 'OPENAI', 'deepseek', 'https://api.deepseek.com', 'REPLACE_WITH_ENCRYPTED_KEY', 10, 10, 'ACTIVE', 0);

INSERT INTO channel_model (channel_id, model_name, model_alias, input_ratio, output_ratio, tier, enabled)
VALUES
    (1, 'deepseek-chat', 'deepseek-chat', 1.0000, 1.0000, 'STANDARD', 1),
    (1, 'deepseek-reasoner', 'deepseek-reasoner', 1.0000, 1.0000, 'PREMIUM', 1);

INSERT INTO system_config (config_key, config_value, description) VALUES
    ('quota_for_new_user', '100000', '新用户赠送额度'),
    ('default_model', 'deepseek-chat', '默认模型'),
    ('register_enabled', 'true', '是否开放注册'),
    ('email_verify_enabled', 'false', '是否需要邮箱验证'),
    ('cache_ttl_hours', '24', '语义缓存默认过期时间'),
    ('similarity_threshold', '0.92', '语义缓存匹配阈值');