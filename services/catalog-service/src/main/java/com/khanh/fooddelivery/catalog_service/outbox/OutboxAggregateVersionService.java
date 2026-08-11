package com.khanh.fooddelivery.catalog_service.outbox;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxAggregateVersionService {
    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.MANDATORY)
    public long nextVersion(String aggregateType, UUID aggregateId) {
        Long aggregateVersion =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO outbox_aggregate_versions (
                            aggregate_type,
                            aggregate_id,
                            aggregate_version
                        )
                        VALUES (?, ?, 1)
                        ON CONFLICT (aggregate_type, aggregate_id)
                        DO UPDATE
                            SET aggregate_version = outbox_aggregate_versions.aggregate_version + 1
                        RETURNING aggregate_version
                        """,
                        Long.class,
                        aggregateType,
                        aggregateId);
        if (aggregateVersion == null) {
            throw new IllegalStateException("Unable to allocate an outbox aggregate version");
        }
        return aggregateVersion;
    }
}
