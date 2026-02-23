package com.example.multids.config.properties;

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

        properties.setDatasources(map);

        assertSame(map, properties.getDatasources());
    }
}
