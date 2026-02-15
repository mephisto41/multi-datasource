package com.example.multids.config.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SingleDatasourcePropertiesTest {

    @Test
    void gettersAndSettersWork() {
        SingleDatasourceProperties properties = new SingleDatasourceProperties();

        properties.setUrl("jdbc:test");
        properties.setUsername("user");
        properties.setPassword("secret");
        properties.setDriverClassName("org.test.Driver");
        properties.setValidationQuery("SELECT 42");
        properties.setMaximumPoolSize(3);

        assertEquals("jdbc:test", properties.getUrl());
        assertEquals("user", properties.getUsername());
        assertEquals("secret", properties.getPassword());
        assertEquals("org.test.Driver", properties.getDriverClassName());
        assertEquals("SELECT 42", properties.getValidationQuery());
        assertEquals(3, properties.getMaximumPoolSize());
    }
}
