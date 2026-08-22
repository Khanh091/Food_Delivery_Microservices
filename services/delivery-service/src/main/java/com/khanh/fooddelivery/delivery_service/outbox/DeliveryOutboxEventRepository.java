package com.khanh.fooddelivery.delivery_service.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryOutboxEventRepository extends JpaRepository<DeliveryOutboxEvent, UUID> {

    boolean existsByAggregateTypeAndAggregateIdAndEventType(
            String aggregateType,
            UUID aggregateId,
            String eventType
    );

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
              AND (claimed_at IS NULL OR claimed_at <= :now)
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<DeliveryOutboxEvent> lockNextPublishable(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );
}
