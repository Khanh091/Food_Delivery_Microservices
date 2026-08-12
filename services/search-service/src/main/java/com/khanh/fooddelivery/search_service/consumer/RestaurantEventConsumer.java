package com.khanh.fooddelivery.search_service.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.search_service.event.RestaurantDomainEventEnvelope;
import com.khanh.fooddelivery.search_service.event.RestaurantEventType;
import com.khanh.fooddelivery.search_service.projection.RestaurantProjectionService;
import java.util.EnumSet;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RestaurantEventConsumer {
    private static final EnumSet<RestaurantEventType> HANDLED = EnumSet.allOf(RestaurantEventType.class);
    private final ObjectMapper mapper; private final RestaurantProjectionService projection;
    public RestaurantEventConsumer(ObjectMapper mapper, RestaurantProjectionService projection) { this.mapper=mapper; this.projection=projection; }
    @KafkaListener(topics="${app.search.restaurant-events-topic}", groupId="${SEARCH_RESTAURANT_CONSUMER_GROUP:search-service-restaurant-indexer}", containerFactory="restaurantKafkaListenerContainerFactory")
    public void consume(String payload) {
        RestaurantDomainEventEnvelope event = deserialize(payload); validate(event);
        if(event.eventVersion()!=2) throw new UnsupportedRestaurantEventVersionException(event.eventVersion());
        RestaurantEventType type;
        try { type=RestaurantEventType.valueOf(event.eventType()); } catch(IllegalArgumentException exception) { throw new InvalidRestaurantEventEnvelopeException("Unknown restaurant event type: "+event.eventType(), exception); }
        if (!HANDLED.contains(type)) return;
        projection.apply(event);
    }
    private RestaurantDomainEventEnvelope deserialize(String payload) { try { return mapper.readValue(payload, RestaurantDomainEventEnvelope.class); } catch(JsonProcessingException exception) { throw new InvalidRestaurantEventEnvelopeException("Invalid restaurant event envelope", exception); } }
    private void validate(RestaurantDomainEventEnvelope event) { if(event.eventId()==null || blank(event.eventType()) || event.eventVersion()<1 || blank(event.aggregateType()) || event.aggregateId()==null || event.aggregateVersion()<1 || event.occurredAt()==null || event.data()==null || !event.data().isObject()) throw new InvalidRestaurantEventEnvelopeException("Restaurant event envelope has missing required fields"); }
    private boolean blank(String value) { return value==null || value.isBlank(); }
}
