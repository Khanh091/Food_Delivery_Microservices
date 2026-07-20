package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantBankAccountRepository
        extends JpaRepository<RestaurantBankAccount, UUID> {
    List<RestaurantBankAccount> findAllByRestaurantIdOrderByCreatedAtAsc(UUID id);

    Optional<RestaurantBankAccount> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    Optional<RestaurantBankAccount> findByRestaurantIdAndDefaultAccountTrue(UUID id);

    boolean existsByRestaurantIdAndBankCodeAndAccountNumber(
            UUID id, String bankCode, String number);

    @Modifying
    @Query(
            "update RestaurantBankAccount b set b.defaultAccount=false where b.restaurant.id=:id"
                    + " and b.defaultAccount=true")
    int clearDefault(@Param("id") UUID restaurantId);
}
