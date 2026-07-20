package com.khanh.fooddelivery.restaurant_service.storage;

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
public class FileStorageProperties {

    private StorageProvider provider = StorageProvider.CLOUDINARY;
    private long maxFileSize = 10_485_760L;
    private List<String> allowedContentTypes =
            new ArrayList<>(List.of("image/jpeg", "image/png", "application/pdf"));
    private Cloudinary cloudinary = new Cloudinary();
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Cloudinary {

        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private String baseFolder = "food-delivery/restaurant-documents";
    }

    @Getter
    @Setter
    public static class S3 {

        private String bucket;
        private String region;
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private boolean pathStyleAccess;
        private String baseFolder = "restaurant-documents";
    }
}
