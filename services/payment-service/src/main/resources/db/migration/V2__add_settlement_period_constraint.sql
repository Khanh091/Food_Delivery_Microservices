CREATE UNIQUE INDEX IF NOT EXISTS uq_settlements_period
    ON settlements(beneficiary_type, beneficiary_id, period_from, period_to);
