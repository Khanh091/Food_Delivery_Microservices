ALTER TABLE fee_policies
    ADD CONSTRAINT fee_policy_effective_range_valid
    CHECK (effective_to IS NULL OR effective_to > effective_from);
