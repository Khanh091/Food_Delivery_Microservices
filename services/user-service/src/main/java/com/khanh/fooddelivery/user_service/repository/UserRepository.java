package com.khanh.fooddelivery.user_service.repository;

import com.khanh.fooddelivery.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakUserId(String keycloakUserId);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    INSERT INTO users (
                        id,
                        keycloak_user_id,
                        username,
                        email,
                        phone_number,
                        full_name,
                        status,
                        created_at,
                        updated_at,
                        created_by,
                        updated_by
                    )
                    VALUES (
                        gen_random_uuid(),
                        :keycloakUserId,
                        :username,
                        :email,
                        :phoneNumber,
                        :fullName,
                        'ACTIVE',
                        CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP,
                        :auditor,
                        :auditor
                    )
                    ON CONFLICT DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertProfileIfAbsent(
            @Param("keycloakUserId") String keycloakUserId,
            @Param("username") String username,
            @Param("email") String email,
            @Param("phoneNumber") String phoneNumber,
            @Param("fullName") String fullName,
            @Param("auditor") String auditor
    );
}
