package com.khanh.fooddelivery.search_service.repository;

import com.khanh.fooddelivery.search_service.config.SearchProperties;
import com.khanh.fooddelivery.search_service.document.RestaurantBranchSearchProjection;
import com.khanh.fooddelivery.search_service.document.RestaurantSearchProjection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Repository
public class ElasticsearchRestaurantSearchProjectionRepository
        implements RestaurantSearchProjectionRepository {

    private static final String RESTAURANT_SCRIPT = """
            if (ctx._source.restaurantAggregateVersion == null
                    || params.aggregateVersion > ctx._source.restaurantAggregateVersion) {
                ctx._source.restaurantId = params.restaurantId;
                ctx._source.name = params.name;
                ctx._source.description = params.description;
                ctx._source.status = params.status;
                ctx._source.restaurantCode = params.restaurantCode;
                ctx._source.logoUrl = params.logoUrl;
                ctx._source.coverImageUrl = params.coverImageUrl;
                ctx._source.restaurantAggregateVersion = params.aggregateVersion;
                ctx._source.restaurantLastEventId = params.lastEventId;

                if (ctx._source.branches == null) {
                    ctx._source.branches = [];
                }
            } else {
                ctx.op = 'noop';
            }
            """;

    private static final String BRANCH_SCRIPT = """
            if (ctx._source.restaurantId == null) {
                ctx._source.restaurantId = params.restaurantId;
            }

            if (ctx._source.branches == null) {
                ctx._source.branches = [];
            }

            int found = -1;

            for (int i = 0; i < ctx._source.branches.size(); i++) {
                if (ctx._source.branches.get(i).branchId == params.branch.branchId) {
                    found = i;
                    break;
                }
            }

            if (found == -1) {
                ctx._source.branches.add(params.branch);
            } else {
                def current = ctx._source.branches.get(found);

                if (current.aggregateVersion == null
                        || params.branch.aggregateVersion > current.aggregateVersion) {
                    ctx._source.branches.set(found, params.branch);
                } else {
                    ctx.op = 'noop';
                }
            }
            """;

    private final RestClient client;
    private final SearchProperties properties;

    public ElasticsearchRestaurantSearchProjectionRepository(
            @Qualifier("searchElasticsearchRestClient") RestClient client,
            SearchProperties properties
    ) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void createIndexIfAbsent() {
        try {
            client.get()
                    .uri(
                            "/{index}",
                            properties.getRestaurantIndexName()
                    )
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw exception;
            }

            create();
        }
    }

    @Override
    public void recreateIndex() {
        try {
            client.delete()
                    .uri(
                            "/{index}",
                            properties.getRestaurantIndexName()
                    )
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw exception;
            }
        }

        create();
    }

    @Override
    public void applyRestaurant(RestaurantSearchProjection projection) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(
                "restaurantId",
                projection.restaurantId().toString()
        );
        params.put(
                "name",
                projection.name()
        );
        params.put(
                "description",
                projection.description()
        );
        params.put(
                "status",
                projection.status()
        );
        params.put(
                "restaurantCode",
                projection.restaurantCode()
        );
        params.put(
                "logoUrl",
                projection.logoUrl()
        );
        params.put(
                "coverImageUrl",
                projection.coverImageUrl()
        );
        params.put(
                "aggregateVersion",
                projection.aggregateVersion()
        );
        params.put(
                "lastEventId",
                projection.lastEventId().toString()
        );

        update(
                projection.restaurantId(),
                RESTAURANT_SCRIPT,
                params,
                Map.of(
                        "restaurantId",
                        projection.restaurantId().toString(),
                        "branches",
                        List.of()
                )
        );
    }

    @Override
    public void applyBranch(
            RestaurantBranchSearchProjection projection,
            UUID restaurantId
    ) {
        Map<String, Object> branch = new LinkedHashMap<>();
        branch.put(
                "branchId",
                projection.branchId().toString()
        );
        branch.put(
                "name",
                projection.name()
        );
        branch.put(
                "status",
                projection.status()
        );
        branch.put(
                "addressLine",
                projection.addressLine()
        );
        branch.put(
                "ward",
                projection.ward()
        );
        branch.put(
                "district",
                projection.district()
        );
        branch.put(
                "city",
                projection.city()
        );
        branch.put(
                "latitude",
                projection.latitude()
        );
        branch.put(
                "longitude",
                projection.longitude()
        );
        branch.put(
                "acceptingOrders",
                projection.acceptingOrders()
        );
        branch.put(
                "aggregateVersion",
                projection.aggregateVersion()
        );
        branch.put(
                "lastEventId",
                projection.lastEventId().toString()
        );

        update(
                restaurantId,
                BRANCH_SCRIPT,
                Map.of(
                        "restaurantId",
                        restaurantId.toString(),
                        "branch",
                        branch
                ),
                Map.of(
                        "restaurantId",
                        restaurantId.toString(),
                        "branches",
                        List.of()
                )
        );
    }

    private void update(
            UUID id,
            String script,
            Map<String, Object> params,
            Map<String, Object> upsert
    ) {
        client.post()
                .uri(
                        "/{index}/_update/{id}",
                        properties.getRestaurantIndexName(),
                        id
                )
                .body(
                        Map.of(
                                "scripted_upsert",
                                true,
                                "script",
                                Map.of(
                                        "lang",
                                        "painless",
                                        "source",
                                        script,
                                        "params",
                                        params
                                ),
                                "upsert",
                                upsert
                        )
                )
                .retrieve()
                .toBodilessEntity();
    }

    private void create() {
        client.put()
                .uri(
                        "/{index}",
                        properties.getRestaurantIndexName()
                )
                .body(
                        Map.of(
                                "settings",
                                Map.of(
                                        "analysis",
                                        Map.of(
                                                "analyzer",
                                                Map.of(
                                                        "folded_text",
                                                        Map.of(
                                                                "type",
                                                                "custom",
                                                                "tokenizer",
                                                                "standard",
                                                                "filter",
                                                                List.of(
                                                                        "lowercase",
                                                                        "asciifolding"
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                "mappings",
                                Map.of(
                                        "dynamic",
                                        "strict",
                                        "properties",
                                        mappings()
                                )
                        )
                )
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> mappings() {
        Map<String, Object> mappings = new LinkedHashMap<>();

        mappings.put(
                "restaurantId",
                Map.of(
                        "type",
                        "keyword"
                )
        );
        mappings.put(
                "name",
                text()
        );
        mappings.put(
                "description",
                Map.of(
                        "type",
                        "text",
                        "analyzer",
                        "folded_text"
                )
        );
        mappings.put(
                "status",
                Map.of(
                        "type",
                        "keyword"
                )
        );
        mappings.put(
                "restaurantCode",
                Map.of(
                        "type",
                        "keyword"
                )
        );
        mappings.put(
                "logoUrl",
                Map.of(
                        "type",
                        "keyword",
                        "index",
                        false
                )
        );
        mappings.put(
                "coverImageUrl",
                Map.of(
                        "type",
                        "keyword",
                        "index",
                        false
                )
        );
        mappings.put(
                "restaurantAggregateVersion",
                Map.of(
                        "type",
                        "long"
                )
        );
        mappings.put(
                "restaurantLastEventId",
                Map.of(
                        "type",
                        "keyword"
                )
        );
        mappings.put(
                "branches",
                Map.of(
                        "type",
                        "nested",
                        "properties",
                        branchMappings()
                )
        );

        return mappings;
    }

    private Map<String, Object> text() {
        return Map.of(
                "type",
                "text",
                "analyzer",
                "folded_text",
                "fields",
                Map.of(
                        "keyword",
                        Map.of(
                                "type",
                                "keyword"
                        )
                )
        );
    }

    private Map<String, Object> branchMappings() {
        Map<String, Object> mappings = new LinkedHashMap<>();

        mappings.put(
                "branchId",
                Map.of(
                        "type",
                        "keyword"
                )
        );
        mappings.put(
                "name",
                text()
        );
        mappings.put(
                "status",
                Map.of(
                        "type",
                        "keyword"
                )
        );
        mappings.put(
                "addressLine",
                Map.of(
                        "type",
                        "text",
                        "analyzer",
                        "folded_text"
                )
        );
        mappings.put(
                "ward",
                Map.of(
                        "type",
                        "keyword"
                )
        );
        mappings.put(
                "district",
                Map.of(
                        "type",
                        "keyword"
                )
        );
        mappings.put(
                "city",
                Map.of(
                        "type",
                        "keyword"
                )
        );
        mappings.put(
                "latitude",
                Map.of(
                        "type",
                        "scaled_float",
                        "scaling_factor",
                        1_000_000
                )
        );
        mappings.put(
                "longitude",
                Map.of(
                        "type",
                        "scaled_float",
                        "scaling_factor",
                        1_000_000
                )
        );
        mappings.put(
                "acceptingOrders",
                Map.of(
                        "type",
                        "boolean"
                )
        );
        mappings.put(
                "aggregateVersion",
                Map.of(
                        "type",
                        "long"
                )
        );
        mappings.put(
                "lastEventId",
                Map.of(
                        "type",
                        "keyword"
                )
        );

        return mappings;
    }
}