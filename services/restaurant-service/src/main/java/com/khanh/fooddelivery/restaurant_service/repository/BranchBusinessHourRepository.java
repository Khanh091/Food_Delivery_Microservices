package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.BranchBusinessHour;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchBusinessHourRepository extends JpaRepository<BranchBusinessHour, UUID> {
    List<BranchBusinessHour> findAllByBranchIdOrderByDayOfWeek(UUID id);

    Optional<BranchBusinessHour> findByBranchIdAndDayOfWeek(UUID id, Short day);
}
