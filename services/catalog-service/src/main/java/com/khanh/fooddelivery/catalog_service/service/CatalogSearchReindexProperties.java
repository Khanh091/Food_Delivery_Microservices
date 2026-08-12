package com.khanh.fooddelivery.catalog_service.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.search-reindex")
public class CatalogSearchReindexProperties {
    private int batchSize = 100;
}
