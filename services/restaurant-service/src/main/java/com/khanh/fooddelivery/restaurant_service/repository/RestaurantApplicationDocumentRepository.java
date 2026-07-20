package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.RestaurantApplicationDocument;
import com.khanh.fooddelivery.restaurant_service.enums.ApplicationDocumentType;
import com.khanh.fooddelivery.restaurant_service.enums.DocumentVerificationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantApplicationDocumentRepository
        extends JpaRepository<RestaurantApplicationDocument, UUID> {
    List<RestaurantApplicationDocument> findAllByApplicationIdOrderByCreatedAtAsc(
            UUID applicationId);

    Optional<RestaurantApplicationDocument> findByIdAndApplicationId(UUID id, UUID applicationId);

    boolean existsByApplicationIdAndDocumentType(UUID id, ApplicationDocumentType type);

    boolean existsByApplicationIdAndDocumentTypeAndVerificationStatus(
            UUID id, ApplicationDocumentType type, DocumentVerificationStatus status);
}
