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
        assertEquals("primary", querySourceName(routingDataSource));
        assertEquals("primary", querySourceName(registry.getRequired("primary")));
        assertEquals("reporting", querySourceName(registry.getRequired("reporting")));
        assertEquals("analytics", querySourceName(registry.getRequired("analytics")));

        HikariDataSource primaryDelegate = (HikariDataSource) registry.getRequired("primary").current();
        primaryDelegate.close();

        assertThrows(SQLException.class, () -> querySourceName(routingDataSource));
        assertEquals("reporting", querySourceName(routingDataSource));

        assertTrue(registry.getRequired("primary").healIfNeeded());
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
