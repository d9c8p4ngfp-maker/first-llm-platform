INSERT INTO channel_model (channel_id, model_name, model_alias, input_ratio, output_ratio, tier, enabled)
SELECT 2, 'text-embedding-3-small', 'text-embedding-3-small', 1.0000, 0.0000, 'STANDARD', 1
WHERE NOT EXISTS (
    SELECT 1 FROM channel_model WHERE model_name = 'text-embedding-3-small'
);

INSERT INTO channel_model (channel_id, model_name, model_alias, input_ratio, output_ratio, tier, enabled)
SELECT 3, 'text-embedding-v4', 'text-embedding-v4', 1.0000, 0.0000, 'STANDARD', 1
WHERE NOT EXISTS (
    SELECT 1 FROM channel_model WHERE model_name = 'text-embedding-v4'
);