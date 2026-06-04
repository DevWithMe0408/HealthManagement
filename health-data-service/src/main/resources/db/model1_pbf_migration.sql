-- Model 1 PBF preparation migration for health-data-service.
-- Target DB: health_db (MySQL).
-- Review before running on production. Take a backup first.

USE health_db;

-- 1. Add calculated_metric_snapshots.method if Hibernate ddl-auto has not added it.
SET @method_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'calculated_metric_snapshots'
      AND column_name = 'method'
);

SET @add_method_column_sql = IF(
    @method_column_exists = 0,
    'ALTER TABLE calculated_metric_snapshots ADD COLUMN method VARCHAR(255)',
    'SELECT ''calculated_metric_snapshots.method already exists'' AS message'
);

PREPARE add_method_column_stmt FROM @add_method_column_sql;
EXECUTE add_method_column_stmt;
DEALLOCATE PREPARE add_method_column_stmt;

-- 2. Rename WAIST data to ABDOMEN for base metric values.
UPDATE base_metric_values
SET indicator_type = 'ABDOMEN'
WHERE indicator_type = 'WAIST';

-- 3. Rename WAIST configs to ABDOMEN safely.
-- If both WAIST and ABDOMEN configs exist for the same user, keep ABDOMEN and remove WAIST.
DELETE waist_config
FROM health_indicator_configs waist_config
JOIN health_indicator_configs abdomen_config
  ON abdomen_config.user_id <=> waist_config.user_id
 AND abdomen_config.indicator_type = 'ABDOMEN'
WHERE waist_config.indicator_type = 'WAIST';

UPDATE health_indicator_configs
SET indicator_type = 'ABDOMEN',
    display_name = 'Vong bung'
WHERE indicator_type = 'WAIST';

-- 4. Add THIGH configs for existing users that do not have it yet.
INSERT INTO health_indicator_configs (
    user_id,
    indicator_type,
    display_name,
    unit_id,
    measurement_frequency,
    is_active
)
SELECT
    u.user_id,
    'THIGH',
    'Vong dui',
    (SELECT id FROM units WHERE code = 'cm' LIMIT 1),
    'MONTHLY',
    TRUE
FROM user_for_health_data u
LEFT JOIN health_indicator_configs existing
  ON existing.user_id = u.user_id
 AND existing.indicator_type = 'THIGH'
WHERE existing.id IS NULL;

-- 5. Backfill old system-calculated PBF snapshots as Navy formula.
UPDATE calculated_metric_snapshots
SET method = 'FORMULA'
WHERE indicator_type = 'PBF'
  AND method IS NULL
  AND source_category = 'CALCULATED';

-- 6. Verification queries.
SELECT indicator_type, COUNT(*) AS row_count
FROM base_metric_values
WHERE indicator_type IN ('WAIST', 'ABDOMEN', 'THIGH')
GROUP BY indicator_type;

SELECT indicator_type, COUNT(*) AS row_count
FROM health_indicator_configs
WHERE indicator_type IN ('WAIST', 'ABDOMEN', 'THIGH')
GROUP BY indicator_type;

SELECT method, source_category, COUNT(*) AS row_count
FROM calculated_metric_snapshots
WHERE indicator_type = 'PBF'
GROUP BY method, source_category;
