package com.khanh.fooddelivery.search_service.controller;

import com.khanh.fooddelivery.search_service.common.response.ApiResponse;
import com.khanh.fooddelivery.search_service.dto.GlobalSearchResult;
import com.khanh.fooddelivery.search_service.dto.SearchPageResponse;
import com.khanh.fooddelivery.search_service.service.GlobalSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@Validated
@RequiredArgsConstructor
public class PublicGlobalSearchController {
    private final GlobalSearchService service;

    @GetMapping
    public ApiResponse<SearchPageResponse<GlobalSearchResult>> search(
            @RequestParam @NotBlank @Size(max = 200) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(
                "Search completed successfully", service.search(q, page, size));
    }
}
