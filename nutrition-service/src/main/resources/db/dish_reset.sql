-- dish_reset.sql
-- Xoa toan bo data 3 bang catalog de chuan bi cho viec import lai

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM dish_ingredients;
DELETE FROM dishes;
DELETE FROM ingredients;

SET FOREIGN_KEY_CHECKS = 1;

-- Verify (mong doi: 0, 0, 0)
SELECT 'ingredients' AS tbl, COUNT(*) AS cnt FROM ingredients
UNION ALL SELECT 'dishes', COUNT(*) FROM dishes
UNION ALL SELECT 'dish_ingredients', COUNT(*) FROM dish_ingredients;
