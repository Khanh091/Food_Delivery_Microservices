package com.khanh.fooddelivery.user_service.storage;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.avatar-storage")
public class AvatarStorageProperties {
    private long maxFileSize = 5_242_880L;
    private List<String> allowedContentTypes = new ArrayList<>(List.of("image/jpeg", "image/png", "image/webp"));
    private Cloudinary cloudinary = new Cloudinary();

    @Getter
    @Setter
    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private String baseFolder = "food-delivery/avatars";
    }
}
