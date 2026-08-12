package com.khanh.fooddelivery.search_service.projection;

import com.khanh.fooddelivery.search_service.repository.SearchProjectionRepository;
import com.khanh.fooddelivery.search_service.repository.RestaurantSearchProjectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ElasticsearchIndexInitializer implements ApplicationRunner {
    private final SearchProjectionRepository projectionRepository;
    private final RestaurantSearchProjectionRepository restaurantProjectionRepository;

    public ElasticsearchIndexInitializer(SearchProjectionRepository projectionRepository, RestaurantSearchProjectionRepository restaurantProjectionRepository) {
        this.projectionRepository = projectionRepository;
        this.restaurantProjectionRepository = restaurantProjectionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            projectionRepository.createIndexIfAbsent();
            restaurantProjectionRepository.createIndexIfAbsent();
        } catch (RuntimeException exception) {
            log.warn("Elasticsearch index initialization failed; search remains unavailable", exception);
        }
    }
}
