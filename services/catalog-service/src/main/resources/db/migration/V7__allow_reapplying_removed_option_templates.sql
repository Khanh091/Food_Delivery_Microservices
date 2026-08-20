ALTER TABLE option_groups
    DROP CONSTRAINT IF EXISTS uk_option_groups_item_name;

CREATE UNIQUE INDEX uk_option_groups_item_active_name
    ON option_groups (item_id, name)
    WHERE status = 'ACTIVE';
