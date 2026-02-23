package com.example.multids.routing;

import com.example.multids.datasource.ManagedDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.SequencedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class HealingRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealingRoutingDataSource.class);

    private final SequencedMap<String, ManagedDataSource> datasources;

    public HealingRoutingDataSource(SequencedMap<String, ManagedDataSource> datasources) {
        this.datasources = Objects.requireNonNull(datasources, "datasources are required");
        if (datasources.isEmpty()) {
            throw new IllegalArgumentException("datasources are required");
        }
    }

    @Override
    protected Object determineCurrentLookupKey() {
        for (var entry : datasources.entrySet()) {
            ManagedDataSource dataSource = entry.getValue();
            if (dataSource.isMarkedHealthy()) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("No healthy datasource available");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnectionWithOnDemandHeal(this::getConnectionInternal);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnectionWithOnDemandHeal(() -> getConnectionInternal(username, password));
    }

    private Connection getConnectionWithOnDemandHeal(ConnectionSupplier supplier) throws SQLException {
        try {
            return supplier.get();
        } catch (IllegalStateException ex) {
            healAllDatasources();
            try {
                return supplier.get();
            } catch (IllegalStateException retryEx) {
                throw noHealthyDatasourceException(retryEx);
            }
        }
    }

    private Connection getConnectionInternal() throws SQLException {
        return super.getConnection();
    }

    private Connection getConnectionInternal(String username, String password) throws SQLException {
        return super.getConnection(username, password);
    }

    private void healAllDatasources() {
        for (ManagedDataSource dataSource : datasources.values()) {
            try {
                dataSource.healIfNeeded();
            } catch (Exception ex) {
                LOGGER.warn("On-demand heal failed for datasource {}", dataSource.getName(), ex);
            }
        }
    }

    private static SQLException noHealthyDatasourceException(IllegalStateException cause) {
        return new SQLException("No healthy datasource available", cause);
    }

    @FunctionalInterface
    private interface ConnectionSupplier {
        Connection get() throws SQLException;
    }
}
