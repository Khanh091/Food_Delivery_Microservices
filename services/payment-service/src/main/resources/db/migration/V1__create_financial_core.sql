CREATE TABLE fee_policies (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    policy_version INTEGER NOT NULL UNIQUE,
    restaurant_commission_rate NUMERIC(7,4) NOT NULL,
    driver_commission_rate NUMERIC(7,4) NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fee_policy_rates_valid CHECK (restaurant_commission_rate >= 0 AND restaurant_commission_rate <= 100 AND driver_commission_rate >= 0 AND driver_commission_rate <= 100)
);
CREATE INDEX idx_fee_policies_effective ON fee_policies(status, effective_from, effective_to);

CREATE TABLE financial_snapshots (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    restaurant_id UUID,
    driver_id UUID,
    fee_policy_id UUID NOT NULL,
    fee_policy_version INTEGER NOT NULL,
    food_gross_amount NUMERIC(19,2) NOT NULL,
    delivery_gross_amount NUMERIC(19,2) NOT NULL,
    restaurant_commission_rate NUMERIC(7,4) NOT NULL,
    restaurant_commission_amount NUMERIC(19,2) NOT NULL,
    restaurant_net_amount NUMERIC(19,2) NOT NULL,
    driver_commission_rate NUMERIC(7,4) NOT NULL,
    driver_commission_amount NUMERIC(19,2) NOT NULL,
    driver_net_amount NUMERIC(19,2) NOT NULL,
    platform_revenue_amount NUMERIC(19,2) NOT NULL,
    customer_payable_amount NUMERIC(19,2) NOT NULL,
    payment_processing_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    currency VARCHAR(12) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_financial_snapshots_status ON financial_snapshots(status, created_at);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    order_id UUID NOT NULL UNIQUE,
    customer_user_id UUID NOT NULL,
    restaurant_id UUID,
    driver_id UUID,
    method VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(12) NOT NULL,
    provider VARCHAR(16) NOT NULL,
    provider_transaction_id VARCHAR(255),
    provider_reference VARCHAR(255),
    failure_code VARCHAR(100),
    failure_message VARCHAR(500),
    paid_at TIMESTAMPTZ,
    collected_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    restaurant_advance_confirmed_at TIMESTAMPTZ,
    cash_collected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_payments_provider_tx ON payments(provider, provider_transaction_id) WHERE provider_transaction_id IS NOT NULL;
CREATE INDEX idx_payments_status ON payments(status, created_at);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    owner_type VARCHAR(32),
    owner_id UUID,
    order_id UUID,
    payment_id UUID,
    settlement_id UUID,
    payout_id UUID,
    entry_type VARCHAR(48) NOT NULL,
    direction VARCHAR(8) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(12) NOT NULL,
    idempotency_reference VARCHAR(200) NOT NULL UNIQUE,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ledger_order ON ledger_entries(order_id, occurred_at);
CREATE INDEX idx_ledger_owner ON ledger_entries(owner_type, owner_id, occurred_at);

CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    beneficiary_type VARCHAR(16) NOT NULL,
    beneficiary_id UUID NOT NULL,
    period_from TIMESTAMPTZ NOT NULL,
    period_to TIMESTAMPTZ NOT NULL,
    gross_amount NUMERIC(19,2) NOT NULL,
    commission_amount NUMERIC(19,2) NOT NULL,
    adjustment_amount NUMERIC(19,2) NOT NULL,
    net_amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalized_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ
);
CREATE INDEX idx_settlements_beneficiary ON settlements(beneficiary_type, beneficiary_id, status, period_from);

CREATE TABLE payouts (
    id UUID PRIMARY KEY,
    settlement_id UUID NOT NULL UNIQUE,
    beneficiary_type VARCHAR(16) NOT NULL,
    beneficiary_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(12) NOT NULL,
    status VARCHAR(16) NOT NULL,
    provider VARCHAR(16) NOT NULL,
    provider_reference VARCHAR(255),
    failure_reason VARCHAR(500),
    payout_destination_snapshot VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMPTZ
);

INSERT INTO fee_policies (id, policy_version, restaurant_commission_rate, driver_commission_rate, effective_from, status)
VALUES ('00000000-0000-0000-0000-000000000001', 1, 30.0000, 30.0000, CURRENT_TIMESTAMP, 'ACTIVE');
