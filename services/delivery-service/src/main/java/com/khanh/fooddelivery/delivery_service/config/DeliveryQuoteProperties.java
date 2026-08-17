package com.khanh.fooddelivery.delivery_service.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "delivery.quote")
public class DeliveryQuoteProperties {
    private Duration ttl = Duration.ofMinutes(5);
    private String currency = "VND";
    private BigDecimal baseFee = new BigDecimal("15000");
    private long includedDistanceMeters = 3000;
    private BigDecimal feePerKm = new BigDecimal("5000");
    private long maximumServiceDistanceMeters = 10000;
    private String pricingPolicyVersion = "dev-v1";
    public Duration getTtl() { return ttl; } public void setTtl(Duration ttl) { this.ttl = ttl; }
    public String getCurrency() { return currency; } public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBaseFee() { return baseFee; } public void setBaseFee(BigDecimal baseFee) { this.baseFee = baseFee; }
    public long getIncludedDistanceMeters() { return includedDistanceMeters; } public void setIncludedDistanceMeters(long value) { includedDistanceMeters = value; }
    public BigDecimal getFeePerKm() { return feePerKm; } public void setFeePerKm(BigDecimal value) { feePerKm = value; }
    public long getMaximumServiceDistanceMeters() { return maximumServiceDistanceMeters; } public void setMaximumServiceDistanceMeters(long value) { maximumServiceDistanceMeters = value; }
    public String getPricingPolicyVersion() { return pricingPolicyVersion; } public void setPricingPolicyVersion(String value) { pricingPolicyVersion = value; }
}
