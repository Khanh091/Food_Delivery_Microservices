package com.khanh.fooddelivery.search_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.khanh.fooddelivery.search_service.client.CatalogSellabilityClient;
import com.khanh.fooddelivery.search_service.client.CatalogSellabilityClient.SellableItemFilterRequest;
import com.khanh.fooddelivery.search_service.dto.CatalogItemSearchResponse;
import com.khanh.fooddelivery.search_service.dto.ItemSearchCriteria;
import com.khanh.fooddelivery.search_service.dto.SearchPageResponse;
import com.khanh.fooddelivery.search_service.exception.SearchApiException;
import com.khanh.fooddelivery.search_service.exception.SearchErrorCode;
import com.khanh.fooddelivery.search_service.repository.SearchProjectionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ElasticsearchCatalogItemSearchService implements CatalogItemSearchService {
    private final SearchProjectionRepository projectionRepository;
    private final CatalogSellabilityClient catalogSellabilityClient;

    public ElasticsearchCatalogItemSearchService(
            SearchProjectionRepository projectionRepository,
            CatalogSellabilityClient catalogSellabilityClient) {
        this.projectionRepository = projectionRepository;
        this.catalogSellabilityClient = catalogSellabilityClient;
    }

    @Override
    public SearchPageResponse<CatalogItemSearchResponse> search(ItemSearchCriteria criteria) {
        if (criteria.minPrice() != null
                && criteria.maxPrice() != null
                && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice must not exceed maxPrice");
        }

        JsonNode response = projectionRepository.search(criteria);
        List<CatalogItemSearchResponse> items = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode source = hit.path("_source");
            JsonNode branch = matchingBranch(source.path("branches"), criteria.branchId());
            if (!branch.isMissingNode()) {
                items.add(toResponse(source, branch));
            }
        }

        Map<UUID, List<UUID>> itemIdsByRestaurant = new LinkedHashMap<>();
        for (CatalogItemSearchResponse item : items) {
            itemIdsByRestaurant
                    .computeIfAbsent(item.restaurantId(), ignored -> new ArrayList<>())
                    .add(item.itemId());
        }
        Map<UUID, Set<UUID>> sellableByRestaurant = new LinkedHashMap<>();
        try {
            for (Map.Entry<UUID, List<UUID>> entry : itemIdsByRestaurant.entrySet()) {
                List<UUID> sellable = catalogSellabilityClient
                        .filterSellableItems(
                                entry.getKey(),
                                criteria.branchId(),
                                new SellableItemFilterRequest(entry.getKey(), entry.getValue()))
                        .data()
                        .itemIds();
                sellableByRestaurant.put(entry.getKey(), Set.copyOf(sellable));
            }
        } catch (RuntimeException exception) {
            throw new SearchApiException(SearchErrorCode.CATALOG_SELLABILITY_UNAVAILABLE, exception);
        }
        items.removeIf(item -> !sellableByRestaurant
                .getOrDefault(item.restaurantId(), Set.of())
                .contains(item.itemId()));

        long totalElements = items.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / criteria.size());
        return new SearchPageResponse<>(items, criteria.page(), criteria.size(), totalElements, totalPages);
    }

    private JsonNode matchingBranch(JsonNode branches, UUID branchId) {
        for (JsonNode branch : branches) {
            if (branchId.toString().equals(branch.path("branchId").asText())) {
                return branch;
            }
        }
        return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }

    private CatalogItemSearchResponse toResponse(JsonNode source, JsonNode branch) {
        return new CatalogItemSearchResponse(
                UUID.fromString(source.path("itemId").asText()),
                UUID.fromString(source.path("restaurantId").asText()),
                UUID.fromString(branch.path("branchId").asText()),
                source.path("name").asText(),
                nullableText(source, "description"),
                source.path("itemType").asText(),
                decimal(branch, "sellingPrice"),
                nullableDecimal(branch, "originalPrice"),
                branch.path("isAvailable").asBoolean(),
                nullableInteger(branch, "availableQuantity"),
                nullableInstant(branch, "soldOutUntil"),
                source.path("vegetarian").asBoolean(),
                nullableInteger(source, "preparationTimeMinutes"));
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        return node.path(field).decimalValue();
    }

    private BigDecimal nullableDecimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    private Integer nullableInteger(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.intValue();
    }

    private Instant nullableInstant(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : Instant.parse(value.asText());
    }
}
