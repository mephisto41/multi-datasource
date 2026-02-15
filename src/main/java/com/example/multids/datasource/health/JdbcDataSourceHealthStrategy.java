package com.example.multids.datasource.health;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class JdbcDataSourceHealthStrategy implements DataSourceHealthStrategy {

    @Override
    public boolean isHealthy(DataSource dataSource, String validationQuery) {
        String query = (validationQuery == null || validationQuery.isBlank()) ? "SELECT 1" : validationQuery;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(query);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
