-- ============================================================================
-- validation_queries.sql
-- Queries de verify data sau khi chay dish_seed.sql
-- Chay tung query qua MySQL Workbench, doi chieu ket qua mong doi
-- ============================================================================

-- Query 1: Count rows (mong doi: 203 / 142 / 757)
SELECT 'ingredients' AS tbl, COUNT(*) AS cnt FROM ingredients
UNION ALL SELECT 'dishes', COUNT(*) FROM dishes
UNION ALL SELECT 'dish_ingredients', COUNT(*) FROM dish_ingredients;

-- Query 2: Phan bo ingredient theo group_code (11 nhom)
-- Mong doi: GIA_VI:43, RAU_LA:31, THIT_DO:26, HAI_SAN:25, TINH_BOT_MI:17,
--           RAU_CU:17, BUA_PHU:14, GIA_CAM:9, DAU_DO:9, TINH_BOT_GAO:7, TRUNG:5
SELECT group_code, COUNT(*) AS cnt
FROM ingredients
GROUP BY group_code
ORDER BY cnt DESC;

-- Query 3: Phan bo dish theo slot_code
-- Mong doi: CHINH:66, COMBO:30, RAU:20, BUA_PHU:14, TINH_BOT:12
SELECT slot_code, COUNT(*) AS cnt
FROM dishes
GROUP BY slot_code
ORDER BY cnt DESC;

-- Query 4: Macro coverage ingredient (151 co macro / 52 NULL)
SELECT
  CASE WHEN kcal_per_100g IS NULL THEN 'NULL' ELSE 'Co macro' END AS macro_status,
  COUNT(*) AS cnt
FROM ingredients
GROUP BY macro_status;

-- Query 5: Orphan check dish_ingredient -> dish (mong doi: 0)
SELECT COUNT(*) AS orphan_dish_count
FROM dish_ingredients di
LEFT JOIN dishes d ON d.id = di.dish_id
WHERE d.id IS NULL;

-- Query 6: Orphan check dish_ingredient -> ingredient (mong doi: 0)
SELECT COUNT(*) AS orphan_ingredient_count
FROM dish_ingredients di
LEFT JOIN ingredients i ON i.id = di.ingredient_id
WHERE i.id IS NULL;

-- Query 7: Sanity check macro dish (mong doi: 0 dong lech > 25%)
SELECT name, slot_code, kcal_per_100g,
  (protein_per_100g * 4 + fat_per_100g * 9 + carb_per_100g * 4) AS kcal_calc,
  ROUND(ABS(kcal_per_100g - (protein_per_100g * 4 + fat_per_100g * 9 + carb_per_100g * 4))
        / kcal_per_100g * 100, 1) AS diff_pct
FROM dishes
WHERE kcal_per_100g > 0
  AND ABS(kcal_per_100g - (protein_per_100g * 4 + fat_per_100g * 9 + carb_per_100g * 4))
      / kcal_per_100g > 0.25
ORDER BY diff_pct DESC;

-- Query 8: Sample load Pho bo voi tat ca ingredient
SELECT d.name AS dish, di.sort_order, i.name AS ingredient,
       di.quantity, di.unit, di.note
FROM dishes d
JOIN dish_ingredients di ON di.dish_id = d.id
JOIN ingredients i ON i.id = di.ingredient_id
WHERE d.name = 'Phở bò'
ORDER BY di.sort_order, i.name;

-- Query 9: Avg ingredient/dish (mong doi ~5.3)
SELECT ROUND(AVG(cnt), 2) AS avg_ingredients_per_dish,
       MIN(cnt) AS min_count, MAX(cnt) AS max_count
FROM (
  SELECT dish_id, COUNT(*) AS cnt
  FROM dish_ingredients
  GROUP BY dish_id
) t;

-- Query 10: Top 20 ingredient duoc dung nhieu nhat
SELECT i.name, i.group_code, COUNT(di.id) AS used_in_n_dishes
FROM ingredients i
JOIN dish_ingredients di ON di.ingredient_id = i.id
GROUP BY i.id, i.name, i.group_code
ORDER BY used_in_n_dishes DESC
LIMIT 20;
