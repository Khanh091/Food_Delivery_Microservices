package com.khanh.fooddelivery.search_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.search_service.common.response.ApiResponse;
import com.khanh.fooddelivery.search_service.dto.GlobalSearchResult;
import com.khanh.fooddelivery.search_service.dto.SearchPageResponse;
import com.khanh.fooddelivery.search_service.service.GlobalSearchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicGlobalSearchControllerTests {
    @Mock private GlobalSearchService service;

    @InjectMocks private PublicGlobalSearchController controller;

    @Test
    void wrapsSearchPageInSuccessResponse() {
        SearchPageResponse<GlobalSearchResult> page =
                new SearchPageResponse<>(List.of(), 0, 20, 0, 0);
        when(service.search("ga ran", 0, 20)).thenReturn(page);

        ApiResponse<SearchPageResponse<GlobalSearchResult>> response =
                controller.search("ga ran", 0, 20);

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("Search completed successfully");
        assertThat(response.data()).isSameAs(page);
        verify(service).search("ga ran", 0, 20);
    }
}
