package com.khanh.fooddelivery.search_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.search_service.client.CatalogSellabilityClient;
import com.khanh.fooddelivery.search_service.client.CatalogSellabilityClient.SellableItemFilterResponse;
import com.khanh.fooddelivery.search_service.common.response.ApiResponse;
import com.khanh.fooddelivery.search_service.dto.GlobalSearchResult;
import com.khanh.fooddelivery.search_service.dto.SearchPageResponse;
import com.khanh.fooddelivery.search_service.repository.GlobalElasticsearchSearchRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticsearchGlobalSearchServiceTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String RESTAURANT = "00000000-0000-0000-0000-000000000001";
    private static final String BRANCH = "00000000-0000-0000-0000-000000000011";

    @Mock private GlobalElasticsearchSearchRepository repository;
    @Mock private CatalogSellabilityClient catalogSellabilityClient;
    @InjectMocks private ElasticsearchGlobalSearchService service;

    @BeforeEach
    void allowIndexedItemsThatCatalogConfirmsAsSellable() {
        when(catalogSellabilityClient.filterSellableItems(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> ApiResponse.success(
                        "ok",
                        new SellableItemFilterResponse(
                                invocation.<CatalogSellabilityClient.SellableItemFilterRequest>getArgument(2)
                                        .itemIds())));
    }

    @Test
    void fillsPreviewForBranchTextMatchWhenNoItemMatches() throws Exception {
        when(repository.searchRestaurants("to hieu")).thenReturn(json("""
                {"hits":{"hits":[{"_score":4,"_source":{"restaurantId":"%s","name":"Quan an","status":"ACTIVE","branches":[{"branchId":"%s","name":"To Hieu","status":"ACTIVE","acceptingOrders":true}]},"inner_hits":{"branches":{"hits":{"hits":[{"_source":{"branchId":"%s","name":"To Hieu","status":"ACTIVE","acceptingOrders":true}}]}}}}]}}""".formatted(RESTAURANT, BRANCH, BRANCH)));
        when(repository.searchItems("to hieu")).thenReturn(empty());
        when(repository.previewItemsForBranches(anyList())).thenReturn(fallback("Phở Bò", "Gà Rán"));
        when(repository.restaurantsByIds(List.of())).thenReturn(null);

        SearchPageResponse<GlobalSearchResult> response = service.search("to hieu", 0, 20);

        assertThat(response.items()).singleElement().satisfies(result -> {
            assertThat(result.matchingItems()).isEmpty();
            assertThat(result.previewItems()).extracting(GlobalSearchResult.PreviewItem::name)
                    .containsExactly("Phở Bò", "Gà Rán");
        });
    }

    @Test
    void keepsMatchedItemsFirstThenFillsWithoutDuplicatesAndCapsAtSix() throws Exception {
        when(repository.searchRestaurants("pho ga")).thenReturn(empty());
        when(repository.searchItems("pho ga")).thenReturn(itemMatches("Phở Gà", "Combo Phở Gà"));
        when(repository.previewItemsForBranches(anyList())).thenReturn(fallback(
                "Phở Gà", "Combo Phở Gà", "Gà Rán", "Mỳ Ý", "Cơm Gà", "Canh Rong Biển", "Trà Đào"));
        when(repository.restaurantsByIds(anyList())).thenReturn(restaurantMetadata());

        SearchPageResponse<GlobalSearchResult> response = service.search("pho ga", 0, 20);

        assertThat(response.items()).singleElement().satisfies(result -> {
            assertThat(result.matchingItems()).extracting(GlobalSearchResult.MatchingItem::name)
                    .containsExactly("Phở Gà", "Combo Phở Gà");
            assertThat(result.previewItems()).extracting(GlobalSearchResult.PreviewItem::name)
                    .containsExactly("Phở Gà", "Combo Phở Gà", "Gà Rán", "Mỳ Ý", "Cơm Gà", "Canh Rong Biển");
        });
    }

    private JsonNode itemMatches(String... names) throws Exception {
        StringBuilder hits = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i > 0) hits.append(',');
            hits.append(itemHit(names[i], i + 100));
        }
        return json("{\"hits\":{\"hits\":[" + hits + "]}}");
    }

    private JsonNode fallback(String... names) throws Exception {
        StringBuilder hits = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i > 0) hits.append(',');
            hits.append(itemHit(names[i], i + 100));
        }
        return json("{\"hits\":{\"hits\":[" + hits + "]}}");
    }

    private String itemHit(String name, int offset) {
        return """
                {"_score":3,"_source":{"itemId":"00000000-0000-0000-0000-%012d","restaurantId":"%s","name":"%s","currency":"VND"},"inner_hits":{"branches":{"hits":{"hits":[{"_source":{"branchId":"%s","branchItemId":"00000000-0000-0000-0000-%012d","isAvailable":true,"sellingPrice":53000}}]}}}}
                """.formatted(offset, RESTAURANT, name, BRANCH, offset + 500);
    }

    private JsonNode restaurantMetadata() throws Exception {
        return json("""
                {"hits":{"hits":[{"_source":{"restaurantId":"%s","name":"Quán Phở","status":"ACTIVE","branches":[{"branchId":"%s","name":"Tô Hiệu","status":"ACTIVE","acceptingOrders":true}]}}]}}
                """.formatted(RESTAURANT, BRANCH));
    }

    private JsonNode empty() throws Exception { return json("{\"hits\":{\"hits\":[]}}"); }
    private JsonNode json(String value) throws Exception { return JSON.readTree(value); }
}
