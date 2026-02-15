package com.example.multids.routing;

import com.example.multids.datasource.ManagedDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.SequencedMap;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class HealingRoutingDataSource extends AbstractRoutingDataSource {

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
        try {
            return super.getConnection();
        } catch (IllegalStateException ex) {
            throw noHealthyDatasourceException(ex);
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        try {
            return super.getConnection(username, password);
        } catch (IllegalStateException ex) {
            throw noHealthyDatasourceException(ex);
        }
    }

    private static SQLException noHealthyDatasourceException(IllegalStateException cause) {
        return new SQLException("No healthy datasource available", cause);
    }
}
