package com.example.multids.datasource.factory;

import com.example.multids.config.properties.SingleDatasourceProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class HikariDataSourceFactory implements DataSourceFactory {

    @Override
    public DataSource create(SingleDatasourceProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        return new HikariDataSource(config);
    }
}
