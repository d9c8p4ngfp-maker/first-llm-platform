CREATE TABLE user_group (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    ratio      DECIMAL(6, 3) NOT NULL DEFAULT 1.000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO user_group (name, ratio) VALUES ('default', 1.000);

ALTER TABLE `user` ADD COLUMN group_id BIGINT;

UPDATE `user` SET group_id = (SELECT id FROM user_group WHERE name = 'default' LIMIT 1)
WHERE group_id IS NULL;

ALTER TABLE `user` ADD CONSTRAINT fk_user_group FOREIGN KEY (group_id) REFERENCES user_group (id);