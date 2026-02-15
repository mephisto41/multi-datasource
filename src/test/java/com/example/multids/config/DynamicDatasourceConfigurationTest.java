package com.example.multids.config;

import com.example.multids.config.properties.DynamicDatasourcesProperties;
import com.example.multids.config.properties.SingleDatasourceProperties;
import com.example.multids.datasource.MultiDataSourceRegistry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

class DynamicDatasourceConfigurationTest {

    private final DynamicDatasourceConfiguration configuration = new DynamicDatasourceConfiguration();

    @Test
    void createsRegistryForConfiguredDatasources() {
        DynamicDatasourcesProperties properties = new DynamicDatasourcesProperties();
        SingleDatasourceProperties one = validDatasource();
        SingleDatasourceProperties two = validDatasource();
        Map<String, SingleDatasourceProperties> map = new LinkedHashMap<>();
        map.put("one", one);
        map.put("two", two);
        properties.setDatasources(map);

        MultiDataSourceRegistry registry = configuration.multiDataSourceRegistry(properties, p -> new StubDataSource(), (d, q) -> true);

        assertEquals(2, registry.datasources().size());
        assertEquals("one", registry.getRequired("one").getName());
        assertEquals("two", registry.getRequired("two").getName());
        assertEquals(List.of("one", "two"), registry.datasources().keySet().stream().toList());
    }

    @Test
    void throwsWhenNoDatasourcesConfigured() {
        DynamicDatasourcesProperties properties = new DynamicDatasourcesProperties();
        properties.setDatasources(Map.of());

        assertThrows(IllegalStateException.class,
                () -> configuration.multiDataSourceRegistry(properties, p -> new StubDataSource(), (d, q) -> true));
    }

    @Test
    void throwsWhenDatasourcesConfigIsNull() {
        DynamicDatasourcesProperties properties = new DynamicDatasourcesProperties();
        properties.setDatasources(null);

        assertThrows(IllegalStateException.class,
                () -> configuration.multiDataSourceRegistry(properties, p -> new StubDataSource(), (d, q) -> true));
    }

    @Test
    void createsRoutingDataSourceWithInferredDefault() {
        DynamicDatasourcesProperties properties = new DynamicDatasourcesProperties();
        properties.setDatasources(Map.of("first", validDatasource()));
        MultiDataSourceRegistry registry =
                configuration.multiDataSourceRegistry(properties, p -> new StubDataSource(), (d, q) -> true);

        DataSource routing = configuration.routingDataSource(registry);
        assertTrue(routing instanceof AbstractRoutingDataSource);
    }

    @Test
    void createsRoutingDataSourceWithoutDefaultDatasourceProperty() {
        DynamicDatasourcesProperties properties = new DynamicDatasourcesProperties();
        properties.setDatasources(Map.of("first", validDatasource()));
        MultiDataSourceRegistry registry =
                configuration.multiDataSourceRegistry(properties, p -> new StubDataSource(), (d, q) -> true);

        DataSource routing = configuration.routingDataSource(registry);
        assertTrue(routing instanceof AbstractRoutingDataSource);
    }

    @Test
    void rejectsDatasourceWithBlankName() {
        DynamicDatasourcesProperties properties = new DynamicDatasourcesProperties();
        SingleDatasourceProperties datasource = new SingleDatasourceProperties();
        datasource.setUrl("jdbc:h2:mem:test");
        datasource.setUsername("sa");
        properties.setDatasources(Map.of(" ", datasource));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> configuration.multiDataSourceRegistry(properties, p -> new StubDataSource(), (d, q) -> true));
        assertEquals("app.datasources contains a blank datasource key", ex.getMessage());
    }

    @Test
    void rejectsDatasourceWithMissingUrl() {
        DynamicDatasourcesProperties properties = new DynamicDatasourcesProperties();
        SingleDatasourceProperties datasource = new SingleDatasourceProperties();
        datasource.setUsername("sa");
        properties.setDatasources(Map.of("primary", datasource));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> configuration.multiDataSourceRegistry(properties, p -> new StubDataSource(), (d, q) -> true));
        assertEquals("app.datasources.primary.url must be set", ex.getMessage());
    }

    @Test
    void rejectsDatasourceWithInvalidPoolSize() {
        DynamicDatasourcesProperties properties = new DynamicDatasourcesProperties();
        SingleDatasourceProperties datasource = new SingleDatasourceProperties();
        datasource.setUrl("jdbc:h2:mem:test");
        datasource.setUsername("sa");
        datasource.setMaximumPoolSize(0);
        properties.setDatasources(Map.of("primary", datasource));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> configuration.multiDataSourceRegistry(properties, p -> new StubDataSource(), (d, q) -> true));
        assertEquals("app.datasources.primary.maximum-pool-size must be >= 1", ex.getMessage());
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

    private static SingleDatasourceProperties validDatasource() {
        SingleDatasourceProperties datasource = new SingleDatasourceProperties();
        datasource.setUrl("jdbc:h2:mem:test");
        datasource.setUsername("sa");
        datasource.setMaximumPoolSize(5);
        return datasource;
    }
}
