INSERT INTO system_config (config_key, config_value, description)
SELECT 'channel_fail_threshold', '5', 'channel auto disable threshold'
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'channel_fail_threshold');