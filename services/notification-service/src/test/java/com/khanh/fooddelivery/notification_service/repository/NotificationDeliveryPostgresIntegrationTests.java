package com.khanh.fooddelivery.notification_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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
class NotificationDeliveryPostgresIntegrationTests {

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
                        connection.prepareStatement("TRUNCATE TABLE notification_deliveries CASCADE")) {
            statement.executeUpdate();
        }
    }

    @Test
    void concurrentSameSourceEventCanPersistOnlyOneNotificationRow() throws Exception {
        UUID eventId = UUID.randomUUID();
        CountDownLatch bothReadEmpty = new CountDownLatch(2);
        CountDownLatch releaseInsert = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(
                    () -> insertAfterRead(eventId, bothReadEmpty, releaseInsert));
            Future<Boolean> second = executor.submit(
                    () -> insertAfterRead(eventId, bothReadEmpty, releaseInsert));

            assertThat(bothReadEmpty.await(10, TimeUnit.SECONDS)).isTrue();
            releaseInsert.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS)).isNotEqualTo(second.get(10, TimeUnit.SECONDS));
        }

        assertThat(countBySourceEventId(eventId)).isOne();
    }

    private static boolean insertAfterRead(
            UUID eventId,
            CountDownLatch bothReadEmpty,
            CountDownLatch releaseInsert
    ) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            assertThat(countBySourceEventId(connection, eventId)).isZero();
            bothReadEmpty.countDown();
            assertThat(releaseInsert.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                insertNotification(connection, eventId);
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                if (!"23505".equals(exception.getSQLState())) {
                    throw exception;
                }
                return false;
            }
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void insertNotification(Connection connection, UUID eventId) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        """
                        INSERT INTO notification_deliveries (
                            id, version, source_event_id, offer_id, delivery_id, driver_id,
                            expires_at, status, attempt_count
                        ) VALUES (?, 0, ?, ?, ?, ?, ?, 'PENDING', 1)
                        """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, eventId);
            statement.setObject(3, UUID.randomUUID());
            statement.setObject(4, UUID.randomUUID());
            statement.setObject(5, UUID.randomUUID());
            statement.setTimestamp(6, Timestamp.from(Instant.now().plusSeconds(45)));
            statement.executeUpdate();
        }
    }

    private static long countBySourceEventId(UUID eventId) throws SQLException {
        try (Connection connection = connection()) {
            return countBySourceEventId(connection, eventId);
        }
    }

    private static long countBySourceEventId(Connection connection, UUID eventId) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "SELECT COUNT(*) FROM notification_deliveries WHERE source_event_id = ?")) {
            statement.setObject(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }
}
