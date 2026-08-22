package com.khanh.fooddelivery.delivery_service.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DeliveryOutboxPostgresIntegrationTests {

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
                                "TRUNCATE TABLE outbox_events, delivery_offers, deliveries CASCADE")) {
            statement.executeUpdate();
        }
    }

    @Test
    void failedOutboxInsertRollsBackOfferInTheSameTransaction() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            insertDelivery(connection, deliveryId);
            insertOffer(connection, offerId, deliveryId);
            try {
                insertInvalidOutboxEvent(connection, offerId);
            } catch (SQLException expected) {
                connection.rollback();
            }
        }

        assertThat(countRows("deliveries", deliveryId)).isZero();
        assertThat(countRows("delivery_offers", offerId)).isZero();
        assertThat(countOutboxRows(offerId)).isZero();
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void insertDelivery(Connection connection, UUID deliveryId) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        """
                        INSERT INTO deliveries (
                            id, version, order_id, restaurant_id, branch_id, customer_id,
                            status, restaurant_name, branch_name, customer_address,
                            created_at, updated_at, restaurant_advance_confirmed,
                            customer_cash_collected, dispatch_attempt_count
                        ) VALUES (?, 0, ?, ?, ?, ?, 'MATCHING', ?, ?, ?, ?, ?, false, false, 0)
                        """)) {
            statement.setObject(1, deliveryId);
            statement.setObject(2, UUID.randomUUID());
            statement.setObject(3, UUID.randomUUID());
            statement.setObject(4, UUID.randomUUID());
            statement.setObject(5, UUID.randomUUID());
            statement.setString(6, "Runtime restaurant");
            statement.setString(7, "Runtime branch");
            statement.setString(8, "Runtime address");
            statement.setTimestamp(9, timestamp(Instant.now()));
            statement.setTimestamp(10, timestamp(Instant.now()));
            statement.executeUpdate();
        }
    }

    private static void insertOffer(Connection connection, UUID offerId, UUID deliveryId)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        """
                        INSERT INTO delivery_offers (
                            id, version, delivery_id, driver_id, status, offered_at, expires_at
                        ) VALUES (?, 0, ?, ?, 'PENDING', ?, ?)
                        """)) {
            statement.setObject(1, offerId);
            statement.setObject(2, deliveryId);
            statement.setObject(3, UUID.randomUUID());
            statement.setTimestamp(4, timestamp(Instant.now()));
            statement.setTimestamp(5, timestamp(Instant.now().plusSeconds(45)));
            statement.executeUpdate();
        }
    }

    private static void insertInvalidOutboxEvent(Connection connection, UUID aggregateId)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        """
                        INSERT INTO outbox_events (
                            id, aggregate_type, aggregate_id, event_type, payload, created_at
                        ) VALUES (?, 'DELIVERY_OFFER', ?, 'DELIVERY_OFFER_CREATED', CAST(? AS jsonb), ?)
                        """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, aggregateId);
            statement.setString(3, "{");
            statement.setTimestamp(4, timestamp(Instant.now()));
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

    private static long countOutboxRows(UUID aggregateId) throws SQLException {
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
        return Timestamp.from(instant);
    }
}
