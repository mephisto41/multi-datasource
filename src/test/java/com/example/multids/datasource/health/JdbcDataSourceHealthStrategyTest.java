package com.example.multids.datasource.health;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class JdbcDataSourceHealthStrategyTest {

    private final JdbcDataSourceHealthStrategy strategy = new JdbcDataSourceHealthStrategy();

    @Test
    void returnsTrueWhenQueryExecutes() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        boolean healthy = strategy.isHealthy(dataSource, "SELECT 2");

        assertTrue(healthy);
        verify(statement).execute("SELECT 2");
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void usesDefaultValidationQueryWhenBlank() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        boolean healthy = strategy.isHealthy(dataSource, "   ");

        assertTrue(healthy);
        verify(statement).execute("SELECT 1");
    }

    @Test
    void returnsFalseOnAnyException() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("boom"));

        assertFalse(strategy.isHealthy(dataSource, null));
    }
}
