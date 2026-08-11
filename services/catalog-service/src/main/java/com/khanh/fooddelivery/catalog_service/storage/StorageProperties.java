package com.khanh.fooddelivery.catalog_service.storage;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private long maxFileSize = 10_485_760L;
    private List<String> allowedContentTypes =
            new ArrayList<>(List.of("image/jpeg", "image/png", "image/webp"));
    private Cloudinary cloudinary = new Cloudinary();

    @Getter
    @Setter
    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private String baseFolder = "food-delivery/catalog-items";
    }
}
