package com.khanh.fooddelivery.search_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.khanh.fooddelivery.search_service.client.CatalogSellabilityClient;
import com.khanh.fooddelivery.search_service.client.CatalogSellabilityClient.SellableItemFilterRequest;
import com.khanh.fooddelivery.search_service.client.CatalogSellabilityClient.SellableItemFilterResponse;
import com.khanh.fooddelivery.search_service.dto.GlobalSearchResult;
import com.khanh.fooddelivery.search_service.dto.SearchPageResponse;
import com.khanh.fooddelivery.search_service.exception.SearchApiException;
import com.khanh.fooddelivery.search_service.exception.SearchErrorCode;
import com.khanh.fooddelivery.search_service.repository.GlobalElasticsearchSearchRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchGlobalSearchService implements GlobalSearchService {
    private static final int PREVIEW_ITEMS_PER_BRANCH = 6;
    private final GlobalElasticsearchSearchRepository repository;
    private final CatalogSellabilityClient catalogSellabilityClient;

    public ElasticsearchGlobalSearchService(
            GlobalElasticsearchSearchRepository repository,
            CatalogSellabilityClient catalogSellabilityClient) {
        this.repository = repository;
        this.catalogSellabilityClient = catalogSellabilityClient;
    }

    @Override
    public SearchPageResponse<GlobalSearchResult> search(String query, int page, int size) {
        Map<Key, Candidate> candidates = new LinkedHashMap<>();
        Map<UUID, JsonNode> restaurants = new HashMap<>();
        addRestaurantMatches(repository.searchRestaurants(query), query, candidates, restaurants);
        JsonNode itemHits = repository.searchItems(query);
        addItemMatches(itemHits, candidates);
        addPreviewFallbacks(repository.previewItemsForBranches(
                candidates.keySet().stream().map(Key::branchId).distinct().toList()), candidates);
        filterCandidatesBySellability(candidates);

        List<UUID> missing = candidates.keySet().stream().map(Key::restaurantId)
                .filter(id -> !restaurants.containsKey(id)).distinct().toList();
        JsonNode metadataHits = repository.restaurantsByIds(missing);
        if (metadataHits != null) addRestaurantMetadata(metadataHits, restaurants);

        List<GlobalSearchResult> all = candidates.values().stream()
                .filter(candidate -> result(candidate, restaurants.get(candidate.key.restaurantId())) != null)
                .sorted(Comparator.comparingDouble(Candidate::score).reversed()
                        .thenComparing(c -> c.key.restaurantId().toString())
                        .thenComparing(c -> c.key.branchId().toString()))
                .map(candidate -> result(candidate, restaurants.get(candidate.key.restaurantId())))
                .toList();
        int from = Math.min(Math.multiplyExact(page, size), all.size());
        int to = Math.min(from + size, all.size());
        long total = all.size();
        return new SearchPageResponse<>(all.subList(from, to), page, size, total,
                total == 0 ? 0 : (int) Math.ceil((double) total / size));
    }

    private void filterCandidatesBySellability(Map<Key, Candidate> candidates) {
        candidates.entrySet().removeIf(entry -> {
            Candidate candidate = entry.getValue();
            List<UUID> itemIds = candidate.itemIds();
            if (itemIds.isEmpty()) return candidate.itemScore > 0;
            try {
                SellableItemFilterResponse response = catalogSellabilityClient
                        .filterSellableItems(
                                entry.getKey().restaurantId(),
                                entry.getKey().branchId(),
                                new SellableItemFilterRequest(
                                        entry.getKey().restaurantId(), itemIds))
                        .data();
                candidate.retainSellable(response == null ? List.of() : response.itemIds());
                return candidate.itemScore > 0
                        && candidate.restaurantScore == 0
                        && !candidate.hasMatchingItems();
            } catch (RuntimeException exception) {
                if (exception instanceof SearchApiException searchApiException) throw searchApiException;
                throw new SearchApiException(
                        SearchErrorCode.CATALOG_SELLABILITY_UNAVAILABLE, exception);
            }
        });
    }

    private void addRestaurantMatches(JsonNode response, String query, Map<Key, Candidate> candidates, Map<UUID, JsonNode> restaurants) {
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode source = hit.path("_source");
            UUID restaurantId = uuid(source, "restaurantId");
            restaurants.put(restaurantId, source);
            double score = hit.path("_score").asDouble();
            if (restaurantTextMatches(source, query)) {
                for (JsonNode branch : source.path("branches")) addRestaurantCandidate(candidates, restaurantId, branch, score);
            } else {
                for (JsonNode branchHit : hit.path("inner_hits").path("branches").path("hits").path("hits")) {
                    addRestaurantCandidate(candidates, restaurantId, branchHit.path("_source"), score);
                }
            }
        }
    }

    private void addRestaurantCandidate(Map<Key, Candidate> candidates, UUID restaurantId, JsonNode branch, double score) {
        if (!"ACTIVE".equals(branch.path("status").asText())) return;
        Candidate candidate = candidates.computeIfAbsent(new Key(restaurantId, uuid(branch, "branchId")), Candidate::new);
        candidate.restaurantScore = Math.max(candidate.restaurantScore, score);
    }

    private void addItemMatches(JsonNode response, Map<Key, Candidate> candidates) {
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode item = hit.path("_source");
            UUID restaurantId = uuid(item, "restaurantId");
            double score = hit.path("_score").asDouble();
            for (JsonNode branchHit : hit.path("inner_hits").path("branches").path("hits").path("hits")) {
                JsonNode branch = branchHit.path("_source");
                if (!branch.path("isAvailable").asBoolean()) continue;
                Candidate candidate = candidates.computeIfAbsent(new Key(restaurantId, uuid(branch, "branchId")), Candidate::new);
                candidate.itemScore = Math.max(candidate.itemScore, score);
                candidate.addMatching(item(item, branch));
            }
        }
    }

    private void addPreviewFallbacks(JsonNode response, Map<Key, Candidate> candidates) {
        if (response == null) return;
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode item = hit.path("_source");
            UUID restaurantId = uuid(item, "restaurantId");
            for (JsonNode branchHit : hit.path("inner_hits").path("branches").path("hits").path("hits")) {
                JsonNode branch = branchHit.path("_source");
                if (!branch.path("isAvailable").asBoolean()) continue;
                Candidate candidate = candidates.get(new Key(restaurantId, uuid(branch, "branchId")));
                if (candidate != null) candidate.addPreview(item(item, branch));
            }
        }
    }

    private GlobalSearchResult.MatchingItem item(JsonNode item, JsonNode branch) {
        return new GlobalSearchResult.MatchingItem(
                uuid(item, "itemId"), uuid(branch, "branchItemId"), item.path("name").asText(),
                decimal(branch, "sellingPrice"), nullableDecimal(branch, "originalPrice"),
                item.path("currency").asText(), nullable(item, "primaryImageUrl"));
    }

    private void addRestaurantMetadata(JsonNode response, Map<UUID, JsonNode> restaurants) {
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode source = hit.path("_source"); restaurants.put(uuid(source, "restaurantId"), source);
        }
    }

    private GlobalSearchResult result(Candidate candidate, JsonNode restaurant) {
        if (restaurant == null || !"ACTIVE".equals(restaurant.path("status").asText())) return null;
        JsonNode branch = null;
        for (JsonNode current : restaurant.path("branches")) if (candidate.key.branchId().toString().equals(current.path("branchId").asText())) { branch = current; break; }
        if (branch == null || !"ACTIVE".equals(branch.path("status").asText())) return null;
        return new GlobalSearchResult(candidate.key.restaurantId(), candidate.key.branchId(), restaurant.path("name").asText(),
                branch.path("name").asText(), nullable(restaurant, "logoUrl"), nullable(restaurant, "coverImageUrl"),
                nullable(branch, "addressLine"), nullable(branch, "ward"), nullable(branch, "district"), nullable(branch, "city"),
                decimal(branch, "latitude"), decimal(branch, "longitude"), branch.path("acceptingOrders").asBoolean(),
                List.copyOf(candidate.matchingItems()), candidate.previewItems());
    }

    private boolean restaurantTextMatches(JsonNode source, String query) {
        String q = fold(query);
        return fold(source.path("name").asText()).contains(q) || fold(nullable(source, "description")).contains(q);
    }
    private String fold(String value) { return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(); }
    private UUID uuid(JsonNode node, String field) { return UUID.fromString(node.path(field).asText()); }
    private String nullable(JsonNode node, String field) { JsonNode value=node.get(field); return value==null||value.isNull()?null:value.asText(); }
    private BigDecimal decimal(JsonNode node, String field) { return node.path(field).decimalValue(); }
    private BigDecimal nullableDecimal(JsonNode node, String field) { JsonNode value=node.get(field); return value==null||value.isNull()?null:value.decimalValue(); }

    private record Key(UUID restaurantId, UUID branchId) {}
    private static class Candidate {
        private final Key key;
        private double restaurantScore;
        private double itemScore;
        private final Map<UUID, GlobalSearchResult.MatchingItem> matchingItems = new LinkedHashMap<>();
        private final Map<UUID, GlobalSearchResult.MatchingItem> previewItems = new LinkedHashMap<>();
        Candidate(Key key) { this.key=key; }
        void addMatching(GlobalSearchResult.MatchingItem item) {
            if (matchingItems.size() < PREVIEW_ITEMS_PER_BRANCH) matchingItems.putIfAbsent(item.itemId(), item);
            addPreview(item);
        }
        void addPreview(GlobalSearchResult.MatchingItem item) {
            if (previewItems.size() < PREVIEW_ITEMS_PER_BRANCH) previewItems.putIfAbsent(item.itemId(), item);
        }
        List<GlobalSearchResult.MatchingItem> matchingItems() { return List.copyOf(matchingItems.values()); }
        List<UUID> itemIds() {
            return java.util.stream.Stream.concat(matchingItems.keySet().stream(), previewItems.keySet().stream())
                    .distinct().toList();
        }
        void retainSellable(List<UUID> sellableItemIds) {
            java.util.Set<UUID> allowed = java.util.Set.copyOf(sellableItemIds);
            matchingItems.entrySet().removeIf(entry -> !allowed.contains(entry.getKey()));
            previewItems.entrySet().removeIf(entry -> !allowed.contains(entry.getKey()));
        }
        boolean hasMatchingItems() { return !matchingItems.isEmpty(); }
        boolean hasSellableItems() { return !matchingItems.isEmpty() || !previewItems.isEmpty(); }
        List<GlobalSearchResult.PreviewItem> previewItems() {
            return previewItems.values().stream().map(item -> new GlobalSearchResult.PreviewItem(
                    item.itemId(), item.branchItemId(), item.name(), item.sellingPrice(), item.originalPrice(), item.currency(), item.imageUrl())).toList();
        }
        double score() { return restaurantScore + itemScore + (matchingItems.size() * 0.01); }
    }
}
