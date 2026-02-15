package com.example.multids.datasource;

import java.util.Map;

public record MultiDataSourceRegistry(Map<String, ManagedDataSource> datasources) {

    public ManagedDataSource getRequired(String name) {
        ManagedDataSource dataSource = datasources.get(name);
        if (dataSource == null) {
            throw new IllegalArgumentException("Unknown datasource: " + name);
        }
        return dataSource;
    }
}
