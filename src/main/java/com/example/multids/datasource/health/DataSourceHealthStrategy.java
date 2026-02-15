package com.example.multids.datasource.health;

import javax.sql.DataSource;

public interface DataSourceHealthStrategy {
    boolean isHealthy(DataSource dataSource, String validationQuery);
}
