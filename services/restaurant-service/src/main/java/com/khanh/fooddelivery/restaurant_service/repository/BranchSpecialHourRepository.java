package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.BranchSpecialHour;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchSpecialHourRepository extends JpaRepository<BranchSpecialHour, UUID> {
    List<BranchSpecialHour> findAllByBranchIdOrderBySpecialDateAsc(UUID id);

    Optional<BranchSpecialHour> findByIdAndBranchId(UUID id, UUID branchId);

    Optional<BranchSpecialHour> findByBranchIdAndSpecialDate(UUID id, LocalDate date);

    boolean existsByBranchIdAndSpecialDate(UUID id, LocalDate date);
}
