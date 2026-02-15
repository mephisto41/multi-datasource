package com.example.multids.datasource.factory;

import com.example.multids.config.properties.SingleDatasourceProperties;
import javax.sql.DataSource;

public interface DataSourceFactory {
    DataSource create(SingleDatasourceProperties properties);
}
