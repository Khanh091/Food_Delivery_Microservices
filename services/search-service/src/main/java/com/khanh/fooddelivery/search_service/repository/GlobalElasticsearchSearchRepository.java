package com.khanh.fooddelivery.search_service.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.khanh.fooddelivery.search_service.config.SearchProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

@Repository
public class GlobalElasticsearchSearchRepository {
    private static final int CANDIDATE_LIMIT = 500;
    private static final int INNER_HIT_LIMIT = 100;
    private final RestClient client;
    private final SearchProperties properties;

    public GlobalElasticsearchSearchRepository(
            @Qualifier("searchElasticsearchRestClient") RestClient client, SearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public JsonNode searchRestaurants(String query) {
        Map<String, Object> restaurantText = Map.of(
                "multi_match", Map.of("query", query, "fields", List.of("name^5", "description")));
        Map<String, Object> branchText = Map.of(
                "nested", Map.of("path", "branches", "query", Map.of("multi_match", Map.of(
                        "query", query,
                        "fields", List.of("branches.name^4", "branches.addressLine^2", "branches.district", "branches.city"))),
                        "inner_hits", Map.of("size", INNER_HIT_LIMIT)));
        Map<String, Object> request = Map.of(
                "size", CANDIDATE_LIMIT,
                "track_total_hits", false,
                "query", Map.of("bool", Map.of(
                        "filter", List.of(Map.of("term", Map.of("status", "ACTIVE"))),
                        "should", List.of(restaurantText, branchText),
                        "minimum_should_match", 1)));
        return client.post().uri("/{index}/_search", properties.getRestaurantIndexName())
                .body(request).retrieve().body(JsonNode.class);
    }

    public JsonNode searchItems(String query) {
        Map<String, Object> branchFilter = Map.of("nested", Map.of("path", "branches", "query", Map.of("bool", Map.of(
                "filter", List.of(Map.of("term", Map.of("branches.isAvailable", true))))), "inner_hits", Map.of("size", INNER_HIT_LIMIT)));
        Map<String, Object> request = Map.of(
                "size", CANDIDATE_LIMIT,
                "track_total_hits", false,
                "query", Map.of("bool", Map.of(
                        "filter", List.of(Map.of("term", Map.of("status", "ACTIVE")), branchFilter),
                        "must", List.of(Map.of("multi_match", Map.of("query", query, "fields", List.of("name^3", "description")))))));
        return client.post().uri("/{index}/_search", properties.getIndexName()).body(request)
                .retrieve().body(JsonNode.class);
    }

    public JsonNode previewItemsForBranches(List<UUID> branchIds) {
        if (branchIds.isEmpty()) return null;
        List<String> values = branchIds.stream().map(UUID::toString).toList();
        Map<String, Object> candidateBranches = Map.of("nested", Map.of(
                "path", "branches",
                "query", Map.of("bool", Map.of("filter", List.of(
                        Map.of("terms", Map.of("branches.branchId", values)),
                        Map.of("term", Map.of("branches.isAvailable", true))))),
                "inner_hits", Map.of("size", INNER_HIT_LIMIT)));
        Map<String, Object> request = Map.of(
                "size", CANDIDATE_LIMIT,
                "track_total_hits", false,
                "sort", List.of(Map.of("name.keyword", Map.of("order", "asc")), Map.of("itemId", Map.of("order", "asc"))),
                "query", Map.of("bool", Map.of("filter", List.of(
                        Map.of("term", Map.of("status", "ACTIVE")), candidateBranches))));
        return client.post().uri("/{index}/_search", properties.getIndexName()).body(request)
                .retrieve().body(JsonNode.class);
    }

    public JsonNode restaurantsByIds(List<UUID> ids) {
        if (ids.isEmpty()) return null;
        List<String> values = ids.stream().map(UUID::toString).toList();
        Map<String, Object> request = Map.of("size", ids.size(), "query", Map.of("terms", Map.of("restaurantId", values)));
        return client.post().uri("/{index}/_search", properties.getRestaurantIndexName()).body(request)
                .retrieve().body(JsonNode.class);
    }
}
