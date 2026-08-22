package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.request.FeePolicyRequest;
import com.khanh.fooddelivery.payment_service.dto.response.FeePolicyResponse;
import com.khanh.fooddelivery.payment_service.entity.FeePolicy;
import java.time.Instant;
import java.util.List;

public interface FeePolicyService {
    FeePolicy currentPolicy(Instant at);

    FeePolicyResponse activePolicy();

    List<FeePolicyResponse> history();

    FeePolicyResponse createPolicy(FeePolicyRequest request);
}
