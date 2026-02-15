package com.example.multids.config;

import com.example.multids.config.properties.DynamicDatasourcesProperties;
import com.example.multids.config.properties.SingleDatasourceProperties;
import com.example.multids.datasource.ManagedDataSource;
import com.example.multids.datasource.MultiDataSourceRegistry;
import com.example.multids.datasource.factory.DataSourceFactory;
import com.example.multids.datasource.health.DataSourceHealthStrategy;
import com.example.multids.routing.HealingRoutingDataSource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DynamicDatasourcesProperties.class)
public class DynamicDatasourceConfiguration {

    @Bean
    public MultiDataSourceRegistry multiDataSourceRegistry(
            DynamicDatasourcesProperties properties,
            DataSourceFactory dataSourceFactory,
            DataSourceHealthStrategy dataSourceHealthStrategy
    ) {
        if (properties.getDatasources() == null || properties.getDatasources().isEmpty()) {
            throw new IllegalStateException("app.datasources must define at least one datasource");
        }

        Map<String, ManagedDataSource> managedDataSources = new LinkedHashMap<>();
        for (Map.Entry<String, SingleDatasourceProperties> entry : properties.getDatasources().entrySet()) {
            validateDatasource(entry.getKey(), entry.getValue());
            managedDataSources.put(
                    entry.getKey(),
                    new ManagedDataSource(entry.getKey(), entry.getValue(), dataSourceFactory, dataSourceHealthStrategy)
            );
        }
        return new MultiDataSourceRegistry(Collections.unmodifiableMap(new LinkedHashMap<>(managedDataSources)));
    }

    @Bean
    @Primary
    public DataSource routingDataSource(
            MultiDataSourceRegistry registry
    ) {
        SequencedMap<String, ManagedDataSource> orderedDataSources = new LinkedHashMap<>();
        Map<Object, Object> targets = new LinkedHashMap<>();
        for (Map.Entry<String, ManagedDataSource> entry : registry.datasources().entrySet()) {
            orderedDataSources.put(entry.getKey(), entry.getValue());
            targets.put(entry.getKey(), entry.getValue());
        }
        HealingRoutingDataSource routingDataSource = new HealingRoutingDataSource(orderedDataSources);
        routingDataSource.setTargetDataSources(targets);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    private static void validateDatasource(String name, SingleDatasourceProperties properties) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("app.datasources contains a blank datasource key");
        }
        if (properties == null) {
            throw new IllegalStateException("app.datasources." + name + " must be configured");
        }
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            throw new IllegalStateException("app.datasources." + name + ".url must be set");
        }
        if (properties.getUsername() == null || properties.getUsername().isBlank()) {
            throw new IllegalStateException("app.datasources." + name + ".username must be set");
        }
        if (properties.getMaximumPoolSize() < 1) {
            throw new IllegalStateException("app.datasources." + name + ".maximum-pool-size must be >= 1");
        }
    }
}
