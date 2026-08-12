package com.khanh.fooddelivery.search_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class SearchServiceApplicationTests {

    @Test
    void isBootApplication() {
        assertThat(SearchServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }

}
