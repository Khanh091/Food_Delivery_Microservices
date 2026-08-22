CREATE OR REPLACE FUNCTION prevent_financial_snapshot_economics_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.order_id IS DISTINCT FROM OLD.order_id
       OR NEW.fee_policy_id IS DISTINCT FROM OLD.fee_policy_id
       OR NEW.fee_policy_version IS DISTINCT FROM OLD.fee_policy_version
       OR NEW.food_gross_amount IS DISTINCT FROM OLD.food_gross_amount
       OR NEW.delivery_gross_amount IS DISTINCT FROM OLD.delivery_gross_amount
       OR NEW.restaurant_commission_rate IS DISTINCT FROM OLD.restaurant_commission_rate
       OR NEW.restaurant_commission_amount IS DISTINCT FROM OLD.restaurant_commission_amount
       OR NEW.restaurant_net_amount IS DISTINCT FROM OLD.restaurant_net_amount
       OR NEW.driver_commission_rate IS DISTINCT FROM OLD.driver_commission_rate
       OR NEW.driver_commission_amount IS DISTINCT FROM OLD.driver_commission_amount
       OR NEW.driver_net_amount IS DISTINCT FROM OLD.driver_net_amount
       OR NEW.platform_revenue_amount IS DISTINCT FROM OLD.platform_revenue_amount
       OR NEW.customer_payable_amount IS DISTINCT FROM OLD.customer_payable_amount
       OR NEW.payment_processing_fee IS DISTINCT FROM OLD.payment_processing_fee
       OR NEW.currency IS DISTINCT FROM OLD.currency THEN
        RAISE EXCEPTION 'Financial snapshot economics are immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER financial_snapshot_economics_immutable
BEFORE UPDATE ON financial_snapshots
FOR EACH ROW
EXECUTE FUNCTION prevent_financial_snapshot_economics_update();

ALTER TABLE settlements
    ADD CONSTRAINT settlement_period_valid CHECK (period_from < period_to);
