package com.khanh.fooddelivery.search_service.controller;

import com.khanh.fooddelivery.search_service.dto.CatalogItemSearchResponse;
import com.khanh.fooddelivery.search_service.dto.ItemSearchCriteria;
import com.khanh.fooddelivery.search_service.dto.SearchPageResponse;
import com.khanh.fooddelivery.search_service.service.CatalogItemSearchService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search/items")
@Validated
public class PublicItemSearchController {
    private final CatalogItemSearchService searchService;

    public PublicItemSearchController(CatalogItemSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchPageResponse<CatalogItemSearchResponse> search(
            @RequestParam UUID branchId,
            @RequestParam(required = false) @Size(max = 200) String q,
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Boolean vegetarian,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return searchService.search(
                new ItemSearchCriteria(
                        branchId,
                        q,
                        restaurantId,
                        itemType,
                        vegetarian,
                        available,
                        minPrice,
                        maxPrice,
                        page,
                        size));
    }
}
