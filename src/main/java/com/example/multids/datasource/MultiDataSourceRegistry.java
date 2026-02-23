package com.example.multids.datasource;

import java.util.Map;

public record MultiDataSourceRegistry(Map<String, ManagedDataSource> datasources) { }
