-- One-time upgrade for an hmdp.sql schema or its existing installations.
-- Resolve any duplicate (user_id, voucher_id) pairs before applying.
ALTER TABLE tb_voucher_order
    ADD UNIQUE KEY uk_order_user_pack (user_id, voucher_id);

ALTER TABLE tb_voucher
    ADD COLUMN token_units BIGINT UNSIGNED NULL COMMENT 'Model tokens included in one pack';

CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    communication_time DATETIME NOT NULL,
    shop_name VARCHAR(128) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
