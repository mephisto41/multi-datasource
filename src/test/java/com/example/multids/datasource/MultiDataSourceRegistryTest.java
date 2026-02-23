package com.example.multids.datasource;

import com.example.multids.config.properties.SingleDatasourceProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MultiDataSourceRegistryTest {

    @Test
    void exposesDatasourcesMap() {
        ManagedDataSource managed = new ManagedDataSource("x", new SingleDatasourceProperties(), p -> new StubDataSource(), (d, q) -> true);
        MultiDataSourceRegistry registry = new MultiDataSourceRegistry(Map.of("x", managed));

        assertEquals(managed, registry.datasources().get("x"));
    }

    private static final class StubDataSource implements javax.sql.DataSource {
        @Override
        public java.sql.Connection getConnection() { return null; }
        @Override
        public java.sql.Connection getConnection(String username, String password) { return null; }
        @Override
        public <T> T unwrap(Class<T> iface) { return null; }
        @Override
        public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override
        public java.io.PrintWriter getLogWriter() { return null; }
        @Override
        public void setLogWriter(java.io.PrintWriter out) { }
        @Override
        public void setLoginTimeout(int seconds) { }
        @Override
        public int getLoginTimeout() { return 0; }
        @Override
        public java.util.logging.Logger getParentLogger() { return java.util.logging.Logger.getGlobal(); }
    }
}
