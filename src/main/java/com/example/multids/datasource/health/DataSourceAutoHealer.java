package com.example.multids.datasource.health;

import com.example.multids.datasource.ManagedDataSource;
import com.example.multids.datasource.MultiDataSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataSourceAutoHealer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceAutoHealer.class);

    private final MultiDataSourceRegistry registry;

    public DataSourceAutoHealer(MultiDataSourceRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${app.auto-heal-delay-ms:5000}")
    public void healAll() {
        for (ManagedDataSource dataSource : registry.datasources().values()) {
            try {
                dataSource.healIfNeeded();
            } catch (Exception ex) {
                LOGGER.warn("Auto-heal failed for datasource {}", dataSource.getName(), ex);
            }
        }
    }
}
