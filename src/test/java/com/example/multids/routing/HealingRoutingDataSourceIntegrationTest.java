package com.example.multids.routing;

import com.example.multids.MultiDatasourceApplication;
import com.example.multids.datasource.ManagedDataSource;
import com.example.multids.datasource.MultiDataSourceRegistry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = MultiDatasourceApplication.class)
class HealingRoutingDataSourceIntegrationTest {

    @Autowired
    private DataSource routingDataSource;

    @Autowired
    private MultiDataSourceRegistry registry;

    @BeforeEach
    void prepareSchemas() throws Exception {
        for (Map.Entry<String, ManagedDataSource> entry : registry.datasources().entrySet()) {
            try (Connection connection = entry.getValue().getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS source_name(name VARCHAR(32))");
                statement.execute("DELETE FROM source_name");
                statement.execute("INSERT INTO source_name(name) VALUES ('" + entry.getKey() + "')");
            }
        }
    }

    @Test
    void routesAcrossThreeDatasourcesWhenPrimaryFailsAndHeals() throws Exception {
        ManagedDataSource primary = registry.datasources().get("primary");
        ManagedDataSource reporting = registry.datasources().get("reporting");
        ManagedDataSource analytics = registry.datasources().get("analytics");

        assertEquals("primary", querySourceName(routingDataSource));
        assertEquals("primary", querySourceName(primary));
        assertEquals("reporting", querySourceName(reporting));
        assertEquals("analytics", querySourceName(analytics));

        HikariDataSource primaryDelegate = (HikariDataSource) primary.current();
        primaryDelegate.close();

        assertThrows(SQLException.class, () -> querySourceName(routingDataSource));
        assertEquals("reporting", querySourceName(routingDataSource));

        assertTrue(primary.healIfNeeded());
        assertEquals("primary", querySourceName(routingDataSource));
    }

    private static String querySourceName(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT name FROM source_name")) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
