package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.response.FinanceSummaryResponse;
import java.time.Instant;

public interface FinanceQueryService {
    FinanceSummaryResponse summary(Instant periodFrom, Instant periodTo);
}
