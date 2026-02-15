package com.example.multids.config.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DynamicDatasourcesPropertiesTest {

    @Test
    void gettersAndSettersWork() {
        DynamicDatasourcesProperties properties = new DynamicDatasourcesProperties();
        Map<String, SingleDatasourceProperties> map = new LinkedHashMap<>();
        SingleDatasourceProperties single = new SingleDatasourceProperties();
        map.put("primary", single);

        properties.setAutoHealDelayMs(1234L);
        properties.setDatasources(map);

        assertEquals(1234L, properties.getAutoHealDelayMs());
        assertSame(map, properties.getDatasources());
    }
}
