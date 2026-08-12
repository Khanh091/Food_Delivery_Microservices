package com.khanh.fooddelivery.search_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {
    private String elasticsearchUri;
    private String indexName;
    private String catalogEventsTopic;
    private String restaurantIndexName;
    private String restaurantEventsTopic;

    public String getElasticsearchUri() {
        return elasticsearchUri;
    }

    public void setElasticsearchUri(String elasticsearchUri) {
        this.elasticsearchUri = elasticsearchUri;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getCatalogEventsTopic() {
        return catalogEventsTopic;
    }

    public void setCatalogEventsTopic(String catalogEventsTopic) {
        this.catalogEventsTopic = catalogEventsTopic;
    }
    public String getRestaurantIndexName() { return restaurantIndexName; }
    public void setRestaurantIndexName(String restaurantIndexName) { this.restaurantIndexName = restaurantIndexName; }
    public String getRestaurantEventsTopic() { return restaurantEventsTopic; }
    public void setRestaurantEventsTopic(String restaurantEventsTopic) { this.restaurantEventsTopic = restaurantEventsTopic; }
}
