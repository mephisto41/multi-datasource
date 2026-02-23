package com.example.multids.config.properties;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class DynamicDatasourcesProperties {

    private Map<String, SingleDatasourceProperties> datasources = new LinkedHashMap<>();

    public Map<String, SingleDatasourceProperties> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, SingleDatasourceProperties> datasources) {
        this.datasources = datasources;
    }
}
