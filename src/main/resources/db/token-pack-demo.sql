-- Optional local fixtures. The provider and pack IDs below are reserved for this demo.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO tb_shop_type (id, name, icon, sort)
VALUES (101, '大模型服务', '', 20);

INSERT INTO tb_shop (id, name, type_id, images, area, address, x, y, sold, comments, score, open_hours)
VALUES (101, '通用文本模型', 101, '', 'API', '演示供应商', 0, 0, 0, 0, 0, '全天'),
       (102, '推理模型', 101, '', 'API', '演示供应商', 0, 0, 0, 0, 0, '全天'),
       (103, '多模态模型', 101, '', 'API', '演示供应商', 0, 0, 0, 0, 0, '全天');

INSERT INTO tb_voucher (id, shop_id, title, sub_title, rules, pay_value, actual_value, token_units, type, status)
VALUES (101, 101, '文本入门包', '适合对话、摘要和文本生成', '同一账号限购一份', 990, 1990, 1000000, 1, 1),
       (102, 102, '推理进阶包', '适合多步推理和代码分析', '同一账号限购一份', 1990, 3990, 3000000, 1, 1),
       (103, 103, '多模态体验包', '适合图片理解和文本生成', '同一账号限购一份', 1590, 2990, 2000000, 1, 1);

INSERT INTO tb_seckill_voucher (voucher_id, stock, begin_time, end_time)
VALUES (101, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL 30 DAY),
       (102, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL 30 DAY),
       (103, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL 30 DAY);

COMMIT;
