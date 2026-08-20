UPDATE option_groups
SET required = (minimum_selections > 0);

ALTER TABLE option_groups
    ADD CONSTRAINT ck_option_groups_required_consistent
    CHECK (required = (minimum_selections > 0));

ALTER TABLE option_groups
    ADD CONSTRAINT ck_option_groups_single_range
    CHECK (selection_type <> 'SINGLE' OR (maximum_selections = 1 AND minimum_selections IN (0, 1)));

ALTER TABLE option_templates
    ADD CONSTRAINT ck_option_templates_single_range
    CHECK (selection_type <> 'SINGLE' OR (maximum_selections = 1 AND minimum_selections IN (0, 1)));
