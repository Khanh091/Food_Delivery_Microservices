package com.khanh.fooddelivery.catalog_service.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(
            value =
                    """
                    SELECT *
                    FROM outbox_events
                    WHERE (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= :now))
                       OR (status = 'PROCESSING' AND next_retry_at <= :now)
                    ORDER BY created_at ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEvent> lockNextPublishable(
            @Param("now") Instant now, @Param("batchSize") int batchSize);
}
