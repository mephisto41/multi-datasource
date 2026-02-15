package com.example.multids.datasource;

import com.example.multids.config.properties.SingleDatasourceProperties;
import com.example.multids.datasource.factory.DataSourceFactory;
import com.example.multids.datasource.health.DataSourceHealthStrategy;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class ManagedDataSource implements DataSource {

    private final String name;
    private final SingleDatasourceProperties properties;
    private final DataSourceFactory dataSourceFactory;
    private final DataSourceHealthStrategy healthStrategy;
    private final AtomicReference<DataSource> delegate;
    private volatile boolean healthy;

    public ManagedDataSource(
            String name,
            SingleDatasourceProperties properties,
            DataSourceFactory dataSourceFactory,
            DataSourceHealthStrategy healthStrategy
    ) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.properties = Objects.requireNonNull(properties, "properties are required");
        this.dataSourceFactory = Objects.requireNonNull(dataSourceFactory, "dataSourceFactory is required");
        this.healthStrategy = Objects.requireNonNull(healthStrategy, "healthStrategy is required");
        this.delegate = new AtomicReference<>(this.dataSourceFactory.create(this.properties));
        this.healthy = healthStrategy.isHealthy(current(), properties.getValidationQuery());
    }

    public String getName() {
        return name;
    }

    public DataSource current() {
        return delegate.get();
    }

    public boolean isHealthy() {
        return refreshHealth();
    }

    public boolean isMarkedHealthy() {
        return healthy;
    }

    public boolean refreshHealth() {
        boolean latestHealth = healthStrategy.isHealthy(current(), properties.getValidationQuery());
        this.healthy = latestHealth;
        return latestHealth;
    }

    public void markUnhealthy() {
        this.healthy = false;
    }

    public boolean healIfNeeded() {
        DataSource active = delegate.get();
        if (healthStrategy.isHealthy(active, properties.getValidationQuery())) {
            healthy = true;
            return false;
        }

        synchronized (this) {
            DataSource latest = delegate.get();
            if (healthStrategy.isHealthy(latest, properties.getValidationQuery())) {
                healthy = true;
                return false;
            }

            DataSource replacement = dataSourceFactory.create(properties);
            delegate.set(replacement);
            boolean replacementHealthy = healthStrategy.isHealthy(replacement, properties.getValidationQuery());
            healthy = replacementHealthy;
            closeQuietly(latest);
            return replacementHealthy;
        }
    }

    void closeQuietly(DataSource dataSource) {
        if (dataSource instanceof AutoCloseable autoCloseable) {
            try {
                autoCloseable.close();
            } catch (Exception ignored) {
                // Best effort resource cleanup.
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            Connection connection = current().getConnection();
            healthy = true;
            return connection;
        } catch (SQLException | RuntimeException ex) {
            markUnhealthy();
            throw ex;
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        try {
            Connection connection = current().getConnection(username, password);
            healthy = true;
            return connection;
        } catch (SQLException | RuntimeException ex) {
            markUnhealthy();
            throw ex;
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return current().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return current().isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return current().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        current().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        current().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return current().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return current().getParentLogger();
    }
}
