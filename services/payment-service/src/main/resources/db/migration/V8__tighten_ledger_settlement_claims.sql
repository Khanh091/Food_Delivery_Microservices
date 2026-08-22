CREATE OR REPLACE FUNCTION prevent_ledger_event_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF (OLD.settlement_id IS NOT NULL AND NEW.settlement_id IS DISTINCT FROM OLD.settlement_id)
       OR NEW.id IS DISTINCT FROM OLD.id
       OR NEW.owner_type IS DISTINCT FROM OLD.owner_type
       OR NEW.owner_id IS DISTINCT FROM OLD.owner_id
       OR NEW.order_id IS DISTINCT FROM OLD.order_id
       OR NEW.payment_id IS DISTINCT FROM OLD.payment_id
       OR NEW.payout_id IS DISTINCT FROM OLD.payout_id
       OR NEW.entry_type IS DISTINCT FROM OLD.entry_type
       OR NEW.direction IS DISTINCT FROM OLD.direction
       OR NEW.amount IS DISTINCT FROM OLD.amount
       OR NEW.currency IS DISTINCT FROM OLD.currency
       OR NEW.idempotency_reference IS DISTINCT FROM OLD.idempotency_reference
       OR NEW.occurred_at IS DISTINCT FROM OLD.occurred_at
       OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'Ledger events are immutable';
    END IF;
    RETURN NEW;
END;
$$;
