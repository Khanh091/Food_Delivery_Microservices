package com.khanh.fooddelivery.payment_service.controller;

import com.khanh.fooddelivery.payment_service.common.response.ApiResponse;
import com.khanh.fooddelivery.payment_service.dto.request.FeePolicyRequest;
import com.khanh.fooddelivery.payment_service.dto.request.SettlementRequest;
import com.khanh.fooddelivery.payment_service.dto.response.FeePolicyResponse;
import com.khanh.fooddelivery.payment_service.dto.response.FinancialFactsResponse;
import com.khanh.fooddelivery.payment_service.dto.response.FinanceSummaryResponse;
import com.khanh.fooddelivery.payment_service.dto.response.PayoutResponse;
import com.khanh.fooddelivery.payment_service.dto.response.SettlementResponse;
import com.khanh.fooddelivery.payment_service.service.FeePolicyService;
import com.khanh.fooddelivery.payment_service.service.FinanceQueryService;
import com.khanh.fooddelivery.payment_service.service.FinancialService;
import com.khanh.fooddelivery.payment_service.service.PayoutService;
import com.khanh.fooddelivery.payment_service.service.SettlementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminFinanceController {
    private final FeePolicyService feePolicies;
    private final FinancialService financials;
    private final FinanceQueryService financeQueries;
    private final SettlementService settlements;
    private final PayoutService payouts;

    @GetMapping("/fee-policies/active")
    public ApiResponse<FeePolicyResponse> activePolicy() {
        return ApiResponse.success("Active fee policy", feePolicies.activePolicy());
    }

    @GetMapping("/fee-policies")
    public ApiResponse<List<FeePolicyResponse>> policies() {
        return ApiResponse.success("Fee policy history", feePolicies.history());
    }

    @PostMapping("/fee-policies")
    public ApiResponse<FeePolicyResponse> create(@Valid @RequestBody FeePolicyRequest request) {
        return ApiResponse.success("Fee policy created", feePolicies.createPolicy(request));
    }

    @GetMapping("/orders/{orderId}/financial-facts")
    public ApiResponse<FinancialFactsResponse> orderFacts(@PathVariable UUID orderId) {
        return ApiResponse.success("Order financial facts", financials.facts(orderId));
    }

    @GetMapping("/summary")
    public ApiResponse<FinanceSummaryResponse> summary(
            @RequestParam(required = false) java.time.Instant periodFrom,
            @RequestParam(required = false) java.time.Instant periodTo) {
        return ApiResponse.success("Finance summary", financeQueries.summary(periodFrom, periodTo));
    }

    @PostMapping("/settlements")
    public ApiResponse<SettlementResponse> settlement(@RequestBody SettlementRequest request) {
        return ApiResponse.success("Settlement created", settlements.create(request));
    }

    @PostMapping("/settlements/{id}/payout")
    public ApiResponse<PayoutResponse> payout(@PathVariable UUID id) {
        return ApiResponse.success("Payout processed", payouts.payout(id));
    }
}
