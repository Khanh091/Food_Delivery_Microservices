package com.khanh.fooddelivery.catalog_service.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class OutboxPostgresIntegrationTests {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @BeforeAll
    static void migrateDatabase() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @BeforeEach
    void clearDatabase() throws SQLException {
        try (Connection connection = connection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "TRUNCATE TABLE outbox_events, outbox_aggregate_versions, catalog_items CASCADE")) {
            statement.executeUpdate();
        }
    }

    @Test
    void skipLockedPreventsTwoWorkersFromClaimingTheSameEvent() throws Exception {
        UUID eventId = insertOutboxEvent(OutboxStatus.PENDING, null);
        CountDownLatch firstWorkerLockedRow = new CountDownLatch(1);
        CountDownLatch releaseFirstWorker = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<List<UUID>> firstWorker =
                    executor.submit(
                            () -> {
                                try (Connection connection = connection()) {
                                    connection.setAutoCommit(false);
                                    List<UUID> claimed = claimPublishable(connection, Instant.now());
                                    firstWorkerLockedRow.countDown();
                                    assertThat(releaseFirstWorker.await(10, TimeUnit.SECONDS)).isTrue();
                                    connection.commit();
                                    return claimed;
                                }
                            });

            assertThat(firstWorkerLockedRow.await(10, TimeUnit.SECONDS)).isTrue();
            Future<List<UUID>> secondWorker =
                    executor.submit(
                            () -> {
                                try (Connection connection = connection()) {
                                    connection.setAutoCommit(false);
                                    List<UUID> claimed = claimPublishable(connection, Instant.now());
                                    connection.commit();
                                    return claimed;
                                }
                            });

            assertThat(secondWorker.get(10, TimeUnit.SECONDS)).isEmpty();
            releaseFirstWorker.countDown();
            assertThat(firstWorker.get(10, TimeUnit.SECONDS)).containsExactly(eventId);
        }
    }

    @Test
    void expiredProcessingLeaseCanBeClaimedAgain() throws Exception {
        UUID eventId = insertOutboxEvent(OutboxStatus.PROCESSING, Instant.now().minusSeconds(1));

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            assertThat(claimPublishable(connection, Instant.now())).containsExactly(eventId);
            connection.commit();
        }
    }

    @Test
    void failedOutboxInsertRollsBackBusinessMutationInTheSameTransaction() throws Exception {
        UUID itemId = UUID.randomUUID();
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            insertCatalogItem(connection, itemId);
            try {
                insertInvalidOutboxEvent(connection, itemId);
            } catch (SQLException expected) {
                connection.rollback();
            }
        }

        assertThat(countRows("catalog_items", itemId)).isZero();
        assertThat(countRowsByAggregateId(itemId)).isZero();
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static UUID insertOutboxEvent(OutboxStatus status, Instant nextRetryAt) throws SQLException {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        Instant now = Instant.now();
        try (Connection connection = connection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO outbox_events (
                                    id, aggregate_type, aggregate_id, event_type, event_version,
                                    aggregate_version, payload, status, retry_count, next_retry_at,
                                    created_at, updated_at, version
                                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?)
                                """)) {
            statement.setObject(1, eventId);
            statement.setString(2, "CATALOG_ITEM");
            statement.setObject(3, aggregateId);
            statement.setString(4, "CATALOG_ITEM_UPSERTED");
            statement.setInt(5, 2);
            statement.setLong(6, 1);
            statement.setString(7, "{}");
            statement.setString(8, status.name());
            statement.setInt(9, 0);
            statement.setTimestamp(10, timestamp(nextRetryAt));
            statement.setTimestamp(11, timestamp(now));
            statement.setTimestamp(12, timestamp(now));
            statement.setLong(13, 0);
            statement.executeUpdate();
        }
        return eventId;
    }

    private static List<UUID> claimPublishable(Connection connection, Instant now) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        """
                        SELECT id
                        FROM outbox_events
                        WHERE (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= ?))
                           OR (status = 'PROCESSING' AND next_retry_at <= ?)
                        ORDER BY created_at ASC
                        LIMIT 50
                        FOR UPDATE SKIP LOCKED
                        """)) {
            statement.setTimestamp(1, timestamp(now));
            statement.setTimestamp(2, timestamp(now));
            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<UUID> ids = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getObject("id", UUID.class));
                }
                return ids;
            }
        }
    }

    private static void insertCatalogItem(Connection connection, UUID itemId) throws SQLException {
        Instant now = Instant.now();
        try (PreparedStatement statement =
                connection.prepareStatement(
                        """
                        INSERT INTO catalog_items (
                            id, restaurant_id, name, item_type, base_price, currency,
                            is_vegetarian, status, created_at, updated_at, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
            statement.setObject(1, itemId);
            statement.setObject(2, UUID.randomUUID());
            statement.setString(3, "Transactional item");
            statement.setString(4, "FOOD");
            statement.setBigDecimal(5, BigDecimal.ONE);
            statement.setString(6, "VND");
            statement.setBoolean(7, false);
            statement.setString(8, "ACTIVE");
            statement.setTimestamp(9, timestamp(now));
            statement.setTimestamp(10, timestamp(now));
            statement.setLong(11, 0);
            statement.executeUpdate();
        }
    }

    private static void insertInvalidOutboxEvent(Connection connection, UUID aggregateId)
            throws SQLException {
        Instant now = Instant.now();
        try (PreparedStatement statement =
                connection.prepareStatement(
                        """
                        INSERT INTO outbox_events (
                            id, aggregate_type, aggregate_id, event_type, event_version,
                            aggregate_version, payload, status, retry_count, created_at, updated_at, version
                        ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?)
                        """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, "CATALOG_ITEM");
            statement.setObject(3, aggregateId);
            statement.setString(4, "CATALOG_ITEM_UPSERTED");
            statement.setInt(5, 2);
            statement.setLong(6, 1);
            statement.setString(7, "{}");
            statement.setString(8, "NOT_A_VALID_OUTBOX_STATUS");
            statement.setInt(9, 0);
            statement.setTimestamp(10, timestamp(now));
            statement.setTimestamp(11, timestamp(now));
            statement.setLong(12, 0);
            statement.executeUpdate();
        }
    }

    private static long countRows(String table, UUID id) throws SQLException {
        try (Connection connection = connection();
                PreparedStatement statement =
                        connection.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE id = ?")) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static long countRowsByAggregateId(UUID aggregateId) throws SQLException {
        try (Connection connection = connection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?")) {
            statement.setObject(1, aggregateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
