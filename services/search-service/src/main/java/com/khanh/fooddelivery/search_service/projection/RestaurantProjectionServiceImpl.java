package com.khanh.fooddelivery.search_service.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.khanh.fooddelivery.search_service.document.RestaurantBranchSearchProjection;
import com.khanh.fooddelivery.search_service.document.RestaurantSearchProjection;
import com.khanh.fooddelivery.search_service.event.RestaurantDomainEventEnvelope;
import com.khanh.fooddelivery.search_service.repository.RestaurantSearchProjectionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RestaurantProjectionServiceImpl implements RestaurantProjectionService {
    private final RestaurantSearchProjectionRepository repository;
    public RestaurantProjectionServiceImpl(RestaurantSearchProjectionRepository repository) { this.repository=repository; }
    @Override public void apply(RestaurantDomainEventEnvelope event) {
        switch(event.eventType()) {
            case "RESTAURANT_UPSERTED", "RESTAURANT_STATUS_CHANGED" -> repository.applyRestaurant(restaurant(event));
            case "RESTAURANT_BRANCH_UPSERTED", "RESTAURANT_BRANCH_STATUS_CHANGED" -> repository.applyBranch(branch(event), uuid(event.data(),"restaurantId"));
            default -> throw new IllegalArgumentException("Unsupported restaurant projection event: "+event.eventType());
        }
    }
    private RestaurantSearchProjection restaurant(RestaurantDomainEventEnvelope e){JsonNode d=e.data();return new RestaurantSearchProjection(uuid(d,"restaurantId"), text(d,"name"), nullable(d,"description"),text(d,"status"),text(d,"restaurantCode"),nullable(d,"logoUrl"),nullable(d,"coverImageUrl"),e.aggregateVersion(),e.eventId());}
    private RestaurantBranchSearchProjection branch(RestaurantDomainEventEnvelope e){JsonNode d=e.data();return new RestaurantBranchSearchProjection(uuid(d,"branchId"),text(d,"name"),text(d,"status"),text(d,"addressLine"),nullable(d,"ward"),nullable(d,"district"),nullable(d,"city"),decimal(d,"latitude"),decimal(d,"longitude"),d.path("acceptingOrders").asBoolean(),e.aggregateVersion(),e.eventId());}
    private UUID uuid(JsonNode d,String f){return UUID.fromString(text(d,f));}
    private String text(JsonNode d,String f){JsonNode n=d.get(f);if(n==null||n.isNull()||n.asText().isBlank())throw new IllegalArgumentException("Missing event field: "+f);return n.asText();}
    private String nullable(JsonNode d,String f){JsonNode n=d.get(f);return n==null||n.isNull()?null:n.asText();}
    private BigDecimal decimal(JsonNode d,String f){JsonNode n=d.get(f);if(n==null||n.isNull())throw new IllegalArgumentException("Missing event field: "+f);return n.decimalValue();}
}
