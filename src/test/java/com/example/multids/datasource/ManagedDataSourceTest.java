package com.example.multids.datasource;

import com.example.multids.config.properties.SingleDatasourceProperties;
import com.example.multids.datasource.factory.DataSourceFactory;
import com.example.multids.datasource.health.DataSourceHealthStrategy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ManagedDataSourceTest {

    @Test
    void constructorRejectsNullArguments() {
        SingleDatasourceProperties properties = new SingleDatasourceProperties();
        DataSourceFactory factory = p -> new TestDataSource();
        DataSourceHealthStrategy health = (d, q) -> true;

        assertThrows(NullPointerException.class, () -> new ManagedDataSource(null, properties, factory, health));
        assertThrows(NullPointerException.class, () -> new ManagedDataSource("name", null, factory, health));
        assertThrows(NullPointerException.class, () -> new ManagedDataSource("name", properties, null, health));
        assertThrows(NullPointerException.class, () -> new ManagedDataSource("name", properties, factory, null));
    }

    @Test
    void healReturnsFalseWhenHealthy() {
        ManagedDataSource managed = new ManagedDataSource(
                "primary",
                new SingleDatasourceProperties(),
                p -> new TestDataSource(),
                (d, q) -> true);

        assertFalse(managed.healIfNeeded());
    }

    @Test
    void healReturnsFalseWhenRecoveredByAnotherThread() {
        SequencedHealthStrategy health = new SequencedHealthStrategy(false, true);
        ManagedDataSource managed = new ManagedDataSource(
                "primary",
                new SingleDatasourceProperties(),
                p -> new TestDataSource(),
                health);

        assertFalse(managed.healIfNeeded());
        assertEquals(2, health.calls.get());
    }

    @Test
    void healReplacesDatasourceAndClosesOldOne() {
        AtomicInteger createCalls = new AtomicInteger();
        TestDataSource first = new TestDataSource();
        TestDataSource second = new TestDataSource();
        ManagedDataSource managed = new ManagedDataSource(
                "primary",
                new SingleDatasourceProperties(),
                p -> createCalls.getAndIncrement() == 0 ? first : second,
                new SequencedHealthStrategy(false, false));

        assertFalse(managed.healIfNeeded());
        assertSame(second, managed.current());
        assertTrue(first.closed);
        assertEquals(2, createCalls.get());
    }

    @Test
    void healReturnsTrueWhenReplacementIsHealthy() {
        AtomicInteger createCalls = new AtomicInteger();
        TestDataSource first = new TestDataSource();
        TestDataSource second = new TestDataSource();
        ManagedDataSource managed = new ManagedDataSource(
                "primary",
                new SingleDatasourceProperties(),
                p -> createCalls.getAndIncrement() == 0 ? first : second,
                new SequencedHealthStrategy(false, false, false, true));

        assertTrue(managed.healIfNeeded());
        assertSame(second, managed.current());
        assertTrue(first.closed);
        assertEquals(2, createCalls.get());
    }

    @Test
    void closeQuietlySwallowsCloseException() {
        ManagedDataSource managed = new ManagedDataSource(
                "primary",
                new SingleDatasourceProperties(),
                p -> new TestDataSource(),
                (d, q) -> true);

        managed.closeQuietly(new ThrowingCloseDataSource());
        managed.closeQuietly(new NonCloseableDataSource());
    }

    @Test
    void delegatesAllDatasourceMethods() throws Exception {
        TestDataSource delegate = new TestDataSource();
        DataSourceFactory factory = p -> delegate;
        ManagedDataSource managed = new ManagedDataSource(
                "primary",
                new SingleDatasourceProperties(),
                factory,
                (d, q) -> true);

        Connection one = managed.getConnection();
        Connection two = managed.getConnection("u", "p");
        Integer unwrapped = managed.unwrap(Integer.class);
        boolean wrapper = managed.isWrapperFor(String.class);
        PrintWriter writer = new PrintWriter(System.out);
        managed.setLogWriter(writer);
        managed.setLoginTimeout(12);

        assertNotNull(one);
        assertNotNull(two);
        assertEquals(9, unwrapped);
        assertTrue(wrapper);
        assertEquals(writer, managed.getLogWriter());
        assertEquals(12, managed.getLoginTimeout());
        assertEquals(Logger.getGlobal(), managed.getParentLogger());
        assertEquals("primary", managed.getName());
    }

    @Test
    void parentLoggerExceptionCanBubbleUp() {
        DataSource bad = Mockito.mock(DataSource.class);
        try {
            Mockito.when(bad.getParentLogger()).thenThrow(new SQLFeatureNotSupportedException("x"));
        } catch (SQLFeatureNotSupportedException ignored) {
            // Mockito setup only.
        }

        ManagedDataSource managed = new ManagedDataSource(
                "primary",
                new SingleDatasourceProperties(),
                p -> bad,
                (d, q) -> true);

        assertThrows(SQLFeatureNotSupportedException.class, managed::getParentLogger);
    }

    @Test
    void marksDatasourceUnhealthyWhenConnectionFails() throws Exception {
        DataSource failing = Mockito.mock(DataSource.class);
        Mockito.when(failing.getConnection()).thenThrow(new SQLException("down"));
        Mockito.when(failing.getConnection(anyString(), anyString())).thenThrow(new SQLException("down"));

        ManagedDataSource managed = new ManagedDataSource(
                "primary",
                new SingleDatasourceProperties(),
                p -> failing,
                (d, q) -> true);

        assertTrue(managed.isMarkedHealthy());
        assertThrows(SQLException.class, managed::getConnection);
        assertFalse(managed.isMarkedHealthy());
    }

    private static final class SequencedHealthStrategy implements DataSourceHealthStrategy {
        private final Deque<Boolean> values = new ArrayDeque<>();
        private final AtomicInteger calls = new AtomicInteger();

        private SequencedHealthStrategy(boolean... responses) {
            for (boolean response : responses) {
                values.addLast(response);
            }
        }

        @Override
        public boolean isHealthy(DataSource dataSource, String validationQuery) {
            calls.incrementAndGet();
            return values.isEmpty() ? false : values.removeFirst();
        }
    }

    private static class TestDataSource implements DataSource, AutoCloseable {
        private final Connection connection = Mockito.mock(Connection.class);
        private PrintWriter logWriter;
        private int timeout;
        private boolean closed;

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return connection;
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return iface.cast(Integer.valueOf(9));
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return true;
        }

        @Override
        public PrintWriter getLogWriter() {
            return logWriter;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            this.logWriter = out;
        }

        @Override
        public void setLoginTimeout(int seconds) {
            this.timeout = seconds;
        }

        @Override
        public int getLoginTimeout() {
            return timeout;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class ThrowingCloseDataSource extends TestDataSource {
        @Override
        public void close() {
            throw new RuntimeException("close failed");
        }
    }

    private static final class NonCloseableDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException { return null; }
        @Override
        public Connection getConnection(String username, String password) throws SQLException { return null; }
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        @Override
        public PrintWriter getLogWriter() throws SQLException { return null; }
        @Override
        public void setLogWriter(PrintWriter out) throws SQLException { }
        @Override
        public void setLoginTimeout(int seconds) throws SQLException { }
        @Override
        public int getLoginTimeout() throws SQLException { return 0; }
        @Override
        public Logger getParentLogger() { return Logger.getGlobal(); }
    }
}
