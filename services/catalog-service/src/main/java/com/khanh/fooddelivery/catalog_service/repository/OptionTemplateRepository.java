package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.OptionTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OptionTemplateRepository extends JpaRepository<OptionTemplate, UUID> {
    Optional<OptionTemplate> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    @Query("""
            select template from OptionTemplate template
            where template.restaurantId = :restaurantId
              and (:query = '' or lower(template.name) like lower(concat('%', :query, '%')))
            order by template.sortOrder asc, template.name asc
            """)
    Page<OptionTemplate> searchByRestaurantId(
            @Param("restaurantId") UUID restaurantId, @Param("query") String query, Pageable pageable);
}
