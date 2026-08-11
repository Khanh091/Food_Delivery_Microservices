package com.khanh.fooddelivery.restaurant_service.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.khanh.fooddelivery.restaurant_service.storage.impl.CloudinaryStorageService;
import com.khanh.fooddelivery.restaurant_service.storage.impl.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class StorageProviderConfigurationTests {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(StorageConfiguration.class);

    @Test
    void usesCloudinaryByDefault() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(FileStorageService.class);
                    assertThat(context.getBean(FileStorageService.class))
                            .isInstanceOf(CloudinaryStorageService.class);
                });
    }

    @Test
    void usesS3WithoutRequiringCredentialsAtStartup() {
        contextRunner
                .withPropertyValues("app.storage.provider=S3")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(FileStorageService.class);
                            assertThat(context.getBean(FileStorageService.class))
                                    .isInstanceOf(S3StorageService.class);
                        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FileStorageProperties.class)
    @Import({CloudinaryStorageService.class, S3StorageService.class})
    static class StorageConfiguration {}
}
