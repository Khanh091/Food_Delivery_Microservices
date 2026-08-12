package com.khanh.fooddelivery.catalog_service.service.impl;

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
class CatalogSearchReindexConcurrencyPostgresIntegrationTests {
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
    void catalogItemSnapshotLockCommitsBeforeConcurrentMutationAndGetsLowerVersion()
            throws Exception {
        UUID itemId = insertCatalogItem("OLD");
        CountDownLatch snapshotLocked = new CountDownLatch(1);
        CountDownLatch releaseSnapshot = new CountDownLatch(1);
        CountDownLatch mutationAttempted = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Void> snapshot =
                    executor.submit(
                            () -> {
                                try (Connection connection = connection()) {
                                    connection.setAutoCommit(false);
                                    String name = lockCatalogItemAndReadName(connection, itemId);
                                    snapshotLocked.countDown();
                                    assertThat(releaseSnapshot.await(10, TimeUnit.SECONDS)).isTrue();
                                    insertOutboxEvent(
                                            connection,
                                            "CATALOG_ITEM",
                                            itemId,
                                            "CATALOG_ITEM_UPSERTED",
                                            "name",
                                            name);
                                    connection.commit();
                                }
                                return null;
                            });

            assertThat(snapshotLocked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<Void> mutation =
                    executor.submit(
                            () -> {
                                try (Connection connection = connection()) {
                                    connection.setAutoCommit(false);
                                    mutationAttempted.countDown();
                                    updateCatalogName(connection, itemId, "NEW");
                                    insertOutboxEvent(
                                            connection,
                                            "CATALOG_ITEM",
                                            itemId,
                                            "CATALOG_ITEM_UPSERTED",
                                            "name",
                                            "NEW");
                                    connection.commit();
                                }
                                return null;
                            });

            assertThat(mutationAttempted.await(10, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(250);
            assertThat(mutation.isDone()).isFalse();
            releaseSnapshot.countDown();
            snapshot.get(10, TimeUnit.SECONDS);
            mutation.get(10, TimeUnit.SECONDS);
        }

        assertThat(eventsFor("CATALOG_ITEM", itemId))
                .containsExactly(new EventState(1, "OLD"), new EventState(2, "NEW"));
    }

    @Test
    void catalogItemSnapshotAfterMutationReadsNewStateAndGetsHigherVersion() throws Exception {
        UUID itemId = insertCatalogItem("OLD");
        try (Connection mutation = connection()) {
            mutation.setAutoCommit(false);
            updateCatalogName(mutation, itemId, "NEW");
            insertOutboxEvent(
                    mutation,
                    "CATALOG_ITEM",
                    itemId,
                    "CATALOG_ITEM_UPSERTED",
                    "name",
                    "NEW");
            mutation.commit();
        }
        try (Connection snapshot = connection()) {
            snapshot.setAutoCommit(false);
            String name = lockCatalogItemAndReadName(snapshot, itemId);
            insertOutboxEvent(
                    snapshot,
                    "CATALOG_ITEM",
                    itemId,
                    "CATALOG_ITEM_UPSERTED",
                    "name",
                    name);
            snapshot.commit();
        }

        assertThat(eventsFor("CATALOG_ITEM", itemId))
                .containsExactly(new EventState(1, "NEW"), new EventState(2, "NEW"));
    }

    @Test
    void branchItemSnapshotLockCommitsBeforeConcurrentPriceMutationAndGetsLowerVersion()
            throws Exception {
        UUID itemId = insertCatalogItem("ITEM");
        UUID branchItemId = insertBranchItem(itemId, new BigDecimal("50000.00"));
        CountDownLatch snapshotLocked = new CountDownLatch(1);
        CountDownLatch releaseSnapshot = new CountDownLatch(1);
        CountDownLatch mutationAttempted = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Void> snapshot =
                    executor.submit(
                            () -> {
                                try (Connection connection = connection()) {
                                    connection.setAutoCommit(false);
                                    BigDecimal price = lockBranchItemAndReadPrice(connection, branchItemId);
                                    snapshotLocked.countDown();
                                    assertThat(releaseSnapshot.await(10, TimeUnit.SECONDS)).isTrue();
                                    insertOutboxEvent(
                                            connection,
                                            "BRANCH_ITEM",
                                            branchItemId,
                                            "BRANCH_ITEM_UPSERTED",
                                            "sellingPrice",
                                            price.toPlainString());
                                    connection.commit();
                                }
                                return null;
                            });

            assertThat(snapshotLocked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<Void> mutation =
                    executor.submit(
                            () -> {
                                try (Connection connection = connection()) {
                                    connection.setAutoCommit(false);
                                    mutationAttempted.countDown();
                                    updateBranchPrice(connection, branchItemId, new BigDecimal("53000.00"));
                                    insertOutboxEvent(
                                            connection,
                                            "BRANCH_ITEM",
                                            branchItemId,
                                            "BRANCH_ITEM_PRICE_CHANGED",
                                            "sellingPrice",
                                            "53000.00");
                                    connection.commit();
                                }
                                return null;
                            });

            assertThat(mutationAttempted.await(10, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(250);
            assertThat(mutation.isDone()).isFalse();
            releaseSnapshot.countDown();
            snapshot.get(10, TimeUnit.SECONDS);
            mutation.get(10, TimeUnit.SECONDS);
        }

        assertThat(eventsFor("BRANCH_ITEM", branchItemId))
                .containsExactly(new EventState(1, "50000.00"), new EventState(2, "53000.00"));
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private UUID insertCatalogItem(String name) throws SQLException {
        UUID itemId = UUID.randomUUID();
        Instant now = Instant.now();
        try (Connection connection = connection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO catalog_items (
                                    id, restaurant_id, name, item_type, base_price, currency,
                                    is_vegetarian, status, created_at, updated_at, version
                                ) VALUES (?, ?, ?, 'FOOD', 1, 'VND', false, 'ACTIVE', ?, ?, 0)
                                """)) {
            statement.setObject(1, itemId);
            statement.setObject(2, UUID.randomUUID());
            statement.setString(3, name);
            statement.setTimestamp(4, Timestamp.from(now));
            statement.setTimestamp(5, Timestamp.from(now));
            statement.executeUpdate();
        }
        return itemId;
    }

    private UUID insertBranchItem(UUID itemId, BigDecimal price) throws SQLException {
        UUID branchItemId = UUID.randomUUID();
        Instant now = Instant.now();
        try (Connection connection = connection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO branch_items (
                                    id, branch_id, item_id, selling_price, is_available,
                                    created_at, updated_at, version
                                ) VALUES (?, ?, ?, ?, true, ?, ?, 0)
                                """)) {
            statement.setObject(1, branchItemId);
            statement.setObject(2, UUID.randomUUID());
            statement.setObject(3, itemId);
            statement.setBigDecimal(4, price);
            statement.setTimestamp(5, Timestamp.from(now));
            statement.setTimestamp(6, Timestamp.from(now));
            statement.executeUpdate();
        }
        return branchItemId;
    }

    private String lockCatalogItemAndReadName(Connection connection, UUID itemId) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT name FROM catalog_items WHERE id = ? FOR UPDATE")) {
            statement.setObject(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString("name");
            }
        }
    }

    private BigDecimal lockBranchItemAndReadPrice(Connection connection, UUID branchItemId)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "SELECT selling_price FROM branch_items WHERE id = ? FOR UPDATE")) {
            statement.setObject(1, branchItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal("selling_price");
            }
        }
    }

    private void updateCatalogName(Connection connection, UUID itemId, String name) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("UPDATE catalog_items SET name = ? WHERE id = ?")) {
            statement.setString(1, name);
            statement.setObject(2, itemId);
            statement.executeUpdate();
        }
    }

    private void updateBranchPrice(Connection connection, UUID branchItemId, BigDecimal price)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("UPDATE branch_items SET selling_price = ? WHERE id = ?")) {
            statement.setBigDecimal(1, price);
            statement.setObject(2, branchItemId);
            statement.executeUpdate();
        }
    }

    private void insertOutboxEvent(
            Connection connection,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payloadField,
            String payloadValue)
            throws SQLException {
        long aggregateVersion = nextAggregateVersion(connection, aggregateType, aggregateId);
        Instant now = Instant.now();
        try (PreparedStatement statement =
                connection.prepareStatement(
                        """
                        INSERT INTO outbox_events (
                            id, aggregate_type, aggregate_id, event_type, event_version,
                            aggregate_version, payload, status, retry_count, created_at, updated_at, version
                        ) VALUES (?, ?, ?, ?, 2, ?, CAST(? AS jsonb), 'PENDING', 0, ?, ?, 0)
                        """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, aggregateType);
            statement.setObject(3, aggregateId);
            statement.setString(4, eventType);
            statement.setLong(5, aggregateVersion);
            statement.setString(6, "{\"" + payloadField + "\":\"" + payloadValue + "\"}");
            statement.setTimestamp(7, Timestamp.from(now));
            statement.setTimestamp(8, Timestamp.from(now));
            statement.executeUpdate();
        }
    }

    private long nextAggregateVersion(Connection connection, String aggregateType, UUID aggregateId)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        """
                        INSERT INTO outbox_aggregate_versions (aggregate_type, aggregate_id, aggregate_version)
                        VALUES (?, ?, 1)
                        ON CONFLICT (aggregate_type, aggregate_id)
                        DO UPDATE SET aggregate_version = outbox_aggregate_versions.aggregate_version + 1
                        RETURNING aggregate_version
                        """)) {
            statement.setString(1, aggregateType);
            statement.setObject(2, aggregateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private List<EventState> eventsFor(String aggregateType, UUID aggregateId) throws SQLException {
        try (Connection connection = connection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT aggregate_version, payload->>'name' AS name, payload->>'sellingPrice' AS price
                                FROM outbox_events
                                WHERE aggregate_type = ? AND aggregate_id = ?
                                ORDER BY aggregate_version
                                """)) {
            statement.setString(1, aggregateType);
            statement.setObject(2, aggregateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<EventState> events = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    events.add(
                            new EventState(
                                    resultSet.getLong("aggregate_version"),
                                    resultSet.getString("name") != null
                                            ? resultSet.getString("name")
                                            : resultSet.getString("price")));
                }
                return events;
            }
        }
    }

    private record EventState(long aggregateVersion, String state) {}
}
