-- Seed data for nutrition-service config tables
-- Run once after tables are created by Hibernate (ddl-auto: update)

-- ===== goal_configs =====
INSERT IGNORE INTO goal_configs (goal_code, cal_multiplier, protein_ratio, fat_ratio, carb_ratio,
    slot_main_ratio, slot_veg_ratio, slot_carb_ratio, weight_p, weight_f, weight_c, weight_kcal, description,
    created_at, updated_at)
VALUES
('GIAM',    0.80, 0.35, 0.30, 0.35, 0.55, 0.15, 0.30, 0.45, 0.20, 0.25, 0.10, 'Giam can',  NOW(), NOW()),
('DUY_TRI', 1.00, 0.25, 0.30, 0.45, 0.50, 0.15, 0.35, 0.30, 0.25, 0.35, 0.10, 'Duy tri',  NOW(), NOW()),
('TANG',    1.15, 0.30, 0.25, 0.45, 0.45, 0.15, 0.40, 0.35, 0.20, 0.35, 0.10, 'Tang can', NOW(), NOW());

-- ===== meal_ratio_configs =====
INSERT IGNORE INTO meal_ratio_configs (plan_type, meal_code, ratio, sort_order, created_at, updated_at) VALUES
('3_BUA', 'SANG',      0.25, 1, NOW(), NOW()),
('3_BUA', 'TRUA',      0.40, 2, NOW(), NOW()),
('3_BUA', 'TOI',       0.35, 3, NOW(), NOW()),
('5_BUA', 'SANG',      0.20, 1, NOW(), NOW()),
('5_BUA', 'PHU_SANG',  0.10, 2, NOW(), NOW()),
('5_BUA', 'TRUA',      0.30, 3, NOW(), NOW()),
('5_BUA', 'PHU_CHIEU', 0.10, 4, NOW(), NOW()),
('5_BUA', 'TOI',       0.30, 5, NOW(), NOW());

-- ===== penalty_configs =====
INSERT IGNORE INTO penalty_configs (layer, distance_days, penalty_value, created_at, updated_at) VALUES
(1, 0, 12, NOW(), NOW()),
(1, 1,  6, NOW(), NOW()),
(1, 2,  3, NOW(), NOW()),
(2, 0,  6, NOW(), NOW()),
(2, 1,  3, NOW(), NOW()),
(2, 2,  1, NOW(), NOW());

-- ===== slot_configs =====
INSERT IGNORE INTO slot_configs (slot_code, slot_factor, min_g, max_g, created_at, updated_at) VALUES
('CHINH',    1.0, 50,  250, NOW(), NOW()),
('RAU',      0.5, 80,  300, NOW(), NOW()),
('TINH_BOT', 0.0, 80,  250, NOW(), NOW()),
('COMBO',    1.0, 100, 400, NOW(), NOW());

-- ===== surplus_penalty_configs =====
INSERT IGNORE INTO surplus_penalty_configs (macro_code, factor, created_at, updated_at) VALUES
('PROTEIN', 0.3, NOW(), NOW()),
('FAT',     0.8, NOW(), NOW()),
('CARB',    0.5, NOW(), NOW()),
('KCAL',    0.7, NOW(), NOW());

-- ===== system_config =====
INSERT IGNORE INTO system_config (config_key, config_value, value_type, description, created_at, updated_at) VALUES
('filter.kcal_tolerance',      '0.15',                          'DECIMAL',    'Bien loc kcal (15%)',              NOW(), NOW()),
('filter.serving_min',         '0.50',                          'DECIMAL',    'Muc serving nho nhat',             NOW(), NOW()),
('filter.serving_max',         '2.00',                          'DECIMAL',    'Muc serving lon nhat',             NOW(), NOW()),
('filter.serving_steps',       '[0.5,0.75,1.0,1.5,2.0]',        'JSON_ARRAY', 'Cac muc serving cho mon thuong', NOW(), NOW()),
('filter.combo_serving_steps', '[0.75,1.0,1.25,1.5]',           'JSON_ARRAY', 'Cac muc serving cho mon combo',   NOW(), NOW()),
('penalty.cap',                '40',                            'INT',        'Penalty toi da cho 1 to hop',      NOW(), NOW()),
('penalty.fav_discount',       '0.5',                           'DECIMAL',    'He so giam penalty cho mon yeu thich', NOW(), NOW()),
('penalty.lookback_days',      '3',                             'INT',        'So ngay nhin lai lich su',         NOW(), NOW()),
('score.threshold',            '0.20',                          'DECIMAL',    'Nguong deviation macro',           NOW(), NOW()),
('reopt.score_threshold',      '50',                            'INT',        'Goi y khi Final Score < nguong nay', NOW(), NOW()),
('reopt.score_drop',           '15',                            'INT',        'Goi y khi score giam > so nay',   NOW(), NOW()),
('display.top_k',              '10',                            'INT',        'So to hop hien thi top',           NOW(), NOW()),
('display.round_step_g',       '25',                            'INT',        'Lam tron serving (gam)',           NOW(), NOW());
