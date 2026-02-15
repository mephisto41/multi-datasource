package com.example.multids.datasource.factory;

import com.example.multids.config.properties.SingleDatasourceProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

class HikariDataSourceFactoryTest {

    private final HikariDataSourceFactory factory = new HikariDataSourceFactory();

    @Test
    void appliesExpectedConfiguration() {
        SingleDatasourceProperties properties = new SingleDatasourceProperties();
        properties.setUrl("jdbc:h2:mem:test");
        properties.setUsername("sa");
        properties.setPassword("p");
        properties.setMaximumPoolSize(7);

        HikariDataSource dataSource = (HikariDataSource) factory.create(properties);
        try {
            assertEquals("jdbc:h2:mem:test", dataSource.getJdbcUrl());
            assertEquals("sa", dataSource.getUsername());
            assertEquals("p", dataSource.getPassword());
            assertEquals(7, dataSource.getMaximumPoolSize());
        } finally {
            dataSource.close();
        }
    }
}
