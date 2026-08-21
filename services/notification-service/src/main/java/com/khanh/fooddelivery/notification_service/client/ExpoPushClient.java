package com.khanh.fooddelivery.notification_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ExpoPushClient {

    private final RestClient client;

    public ExpoPushClient(@Value("${notification.expo.base-url:https://exp.host/--/api/v2}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public DeliveryResult send(String token, String title, String body, Map<String, Object> data) {
        try {
            JsonNode response = client.post()
                    .uri("/push/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(Map.of(
                            "to", token,
                            "title", title,
                            "body", body,
                            "data", data,
                            "sound", "default",
                            "channelId", "delivery-offers",
                            "priority", "high"
                    )))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode result = response == null ? null : response.path("data").path(0);
            String error = result == null ? null : result.path("details").path("error").asText(null);
            if ("DeviceNotRegistered".equals(error) || "InvalidCredentials".equals(error)) {
                return DeliveryResult.INVALID_TOKEN;
            }
            return result != null && "ok".equals(result.path("status").asText())
                    ? DeliveryResult.SENT
                    : DeliveryResult.TRANSIENT_FAILURE;
        } catch (RestClientException exception) {
            return DeliveryResult.TRANSIENT_FAILURE;
        }
    }

    public enum DeliveryResult {
        SENT,
        INVALID_TOKEN,
        TRANSIENT_FAILURE
    }
}
