package com.khanh.fooddelivery.restaurant_service.repository;
import com.khanh.fooddelivery.restaurant_service.entity.BranchBusinessHour; import org.springframework.data.jpa.repository.JpaRepository; import java.time.DayOfWeek; import java.util.*;
public interface BranchBusinessHourRepository extends JpaRepository<BranchBusinessHour,UUID>{List<BranchBusinessHour> findAllByBranchIdOrderByDayOfWeek(UUID id);Optional<BranchBusinessHour> findByBranchIdAndDayOfWeek(UUID id,Short day);}
