package com.khanh.fooddelivery.search_service.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.khanh.fooddelivery.search_service.config.SearchProperties;
import com.khanh.fooddelivery.search_service.document.BranchSearchProjection;
import com.khanh.fooddelivery.search_service.document.CatalogItemSearchProjection;
import com.khanh.fooddelivery.search_service.dto.ItemSearchCriteria;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Repository
public class ElasticsearchSearchProjectionRepository implements SearchProjectionRepository {
    private static final String CATALOG_ITEM_UPDATE_SCRIPT =
            """
            if (ctx._source.catalogItemAggregateVersion == null || params.aggregateVersion > ctx._source.catalogItemAggregateVersion) {
                ctx._source.itemId = params.itemId;
                ctx._source.restaurantId = params.restaurantId;
                ctx._source.name = params.name;
                ctx._source.description = params.description;
                ctx._source.itemType = params.itemType;
                ctx._source.basePrice = params.basePrice;
                ctx._source.currency = params.currency;
                ctx._source.preparationTimeMinutes = params.preparationTimeMinutes;
                ctx._source.vegetarian = params.vegetarian;
                ctx._source.status = params.status;
                ctx._source.catalogItemAggregateVersion = params.aggregateVersion;
                ctx._source.catalogItemLastEventId = params.lastEventId;
                if (ctx._source.branches == null) { ctx._source.branches = []; }
            } else {
                ctx.op = 'noop';
            }
            """;

    private static final String BRANCH_ITEM_UPDATE_SCRIPT =
            """
            if (ctx._source.itemId == null) { ctx._source.itemId = params.itemId; }
            if (ctx._source.branches == null) { ctx._source.branches = []; }
            int branchIndex = -1;
            for (int i = 0; i < ctx._source.branches.size(); i++) {
                if (ctx._source.branches.get(i).branchItemId == params.branch.branchItemId) {
                    branchIndex = i;
                    break;
                }
            }
            if (branchIndex == -1) {
                ctx._source.branches.add(params.branch);
            } else {
                def current = ctx._source.branches.get(branchIndex);
                if (current.aggregateVersion == null || params.branch.aggregateVersion > current.aggregateVersion) {
                    ctx._source.branches.set(branchIndex, params.branch);
                } else {
                    ctx.op = 'noop';
                }
            }
            """;

    private final RestClient restClient;
    private final SearchProperties properties;

    public ElasticsearchSearchProjectionRepository(
            @Qualifier("searchElasticsearchRestClient") RestClient elasticsearchRestClient,
            SearchProperties properties) {
        this.restClient = elasticsearchRestClient;
        this.properties = properties;
    }

    @Override
    public void createIndexIfAbsent() {
        try {
            restClient.get().uri("/{index}", properties.getIndexName()).retrieve().toBodilessEntity();
            return;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw exception;
            }
        }

        restClient
                .put()
                .uri("/{index}", properties.getIndexName())
                .body(indexDefinition())
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void applyCatalogItem(CatalogItemSearchProjection projection) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("itemId", projection.itemId().toString());
        params.put("restaurantId", projection.restaurantId().toString());
        params.put("name", projection.name());
        params.put("description", projection.description());
        params.put("itemType", projection.itemType());
        params.put("basePrice", projection.basePrice());
        params.put("currency", projection.currency());
        params.put("preparationTimeMinutes", projection.preparationTimeMinutes());
        params.put("vegetarian", projection.vegetarian());
        params.put("status", projection.status());
        params.put("aggregateVersion", projection.aggregateVersion());
        params.put("lastEventId", projection.lastEventId().toString());
        update(
                projection.itemId(),
                CATALOG_ITEM_UPDATE_SCRIPT,
                params,
                Map.of("itemId", projection.itemId().toString(), "branches", List.of()));
    }

    @Override
    public void applyBranchItem(UUID itemId, BranchSearchProjection projection) {
        Map<String, Object> branch = new LinkedHashMap<>();
        branch.put("branchItemId", projection.branchItemId().toString());
        branch.put("branchId", projection.branchId().toString());
        branch.put("sellingPrice", projection.sellingPrice());
        branch.put("originalPrice", projection.originalPrice());
        branch.put("isAvailable", projection.isAvailable());
        branch.put("availableQuantity", projection.availableQuantity());
        branch.put("soldOutUntil", projection.soldOutUntil());
        branch.put("aggregateVersion", projection.aggregateVersion());
        branch.put("lastEventId", projection.lastEventId().toString());
        update(itemId, BRANCH_ITEM_UPDATE_SCRIPT, Map.of("itemId", itemId.toString(), "branch", branch), Map.of("itemId", itemId.toString(), "branches", List.of()));
    }

    @Override
    public JsonNode search(ItemSearchCriteria criteria) {
        List<Map<String, Object>> branchConditions = new ArrayList<>();
        branchConditions.add(term("branches.branchId", criteria.branchId().toString()));
        if (criteria.available() != null) {
            branchConditions.add(term("branches.isAvailable", criteria.available()));
        }
        if (criteria.minPrice() != null || criteria.maxPrice() != null) {
            Map<String, Object> range = new LinkedHashMap<>();
            if (criteria.minPrice() != null) {
                range.put("gte", criteria.minPrice());
            }
            if (criteria.maxPrice() != null) {
                range.put("lte", criteria.maxPrice());
            }
            branchConditions.add(Map.of("range", Map.of("branches.sellingPrice", range)));
        }

        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(term("status", "ACTIVE"));
        filters.add(
                Map.of(
                        "nested",
                        Map.of(
                                "path",
                                "branches",
                                "query",
                                Map.of("bool", Map.of("filter", branchConditions)))));
        if (criteria.restaurantId() != null) {
            filters.add(term("restaurantId", criteria.restaurantId().toString()));
        }
        if (criteria.itemType() != null) {
            filters.add(term("itemType", criteria.itemType()));
        }
        if (criteria.vegetarian() != null) {
            filters.add(term("vegetarian", criteria.vegetarian()));
        }

        Map<String, Object> boolQuery = new LinkedHashMap<>();
        boolQuery.put("filter", filters);
        if (criteria.query() != null && !criteria.query().isBlank()) {
            boolQuery.put(
                    "must",
                    List.of(
                            Map.of(
                                    "multi_match",
                                    Map.of(
                                            "query", criteria.query(),
                                            "fields", List.of("name^3", "description")))));
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("from", Math.multiplyExact(criteria.page(), criteria.size()));
        request.put("size", criteria.size());
        request.put("track_total_hits", true);
        request.put("query", Map.of("bool", boolQuery));
        return restClient
                .post()
                .uri("/{index}/_search", properties.getIndexName())
                .body(request)
                .retrieve()
                .body(JsonNode.class);
    }

    private void update(UUID itemId, String script, Map<String, Object> params, Map<String, Object> upsert) {
        Map<String, Object> request =
                Map.of(
                        "scripted_upsert",
                        true,
                        "script",
                        Map.of("lang", "painless", "source", script, "params", params),
                        "upsert",
                        upsert);
        restClient
                .post()
                .uri("/{index}/_update/{id}", properties.getIndexName(), itemId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> term(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private Map<String, Object> indexDefinition() {
        return Map.of(
                "settings",
                Map.of(
                        "analysis",
                        Map.of(
                                "analyzer",
                                Map.of(
                                        "folded_text",
                                        Map.of(
                                                "type", "custom",
                                                "tokenizer", "standard",
                                                "filter", List.of("lowercase", "asciifolding"))))),
                "mappings",
                Map.of("dynamic", "strict", "properties", mappings()));
    }

    private Map<String, Object> mappings() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("itemId", Map.of("type", "keyword"));
        properties.put("restaurantId", Map.of("type", "keyword"));
        properties.put(
                "name",
                Map.of(
                        "type",
                        "text",
                        "analyzer",
                        "folded_text",
                        "fields",
                        Map.of("keyword", Map.of("type", "keyword"))));
        properties.put("description", Map.of("type", "text", "analyzer", "folded_text"));
        properties.put("itemType", Map.of("type", "keyword"));
        properties.put("basePrice", Map.of("type", "scaled_float", "scaling_factor", 100));
        properties.put("currency", Map.of("type", "keyword"));
        properties.put("preparationTimeMinutes", Map.of("type", "integer"));
        properties.put("vegetarian", Map.of("type", "boolean"));
        properties.put("status", Map.of("type", "keyword"));
        properties.put("catalogItemAggregateVersion", Map.of("type", "long"));
        properties.put("catalogItemLastEventId", Map.of("type", "keyword"));
        properties.put("branches", Map.of("type", "nested", "properties", branchMappings()));
        return properties;
    }

    private Map<String, Object> branchMappings() {
        return Map.of(
                "branchItemId", Map.of("type", "keyword"),
                "branchId", Map.of("type", "keyword"),
                "sellingPrice", Map.of("type", "scaled_float", "scaling_factor", 100),
                "originalPrice", Map.of("type", "scaled_float", "scaling_factor", 100),
                "isAvailable", Map.of("type", "boolean"),
                "availableQuantity", Map.of("type", "integer"),
                "soldOutUntil", Map.of("type", "date"),
                "aggregateVersion", Map.of("type", "long"),
                "lastEventId", Map.of("type", "keyword"));
    }
}
