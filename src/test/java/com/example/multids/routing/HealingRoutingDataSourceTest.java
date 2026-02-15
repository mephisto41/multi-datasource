package com.example.multids.routing;

import com.example.multids.datasource.ManagedDataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class HealingRoutingDataSourceTest {

    @Test
    void returnsFirstHealthyDatasource() {
        ManagedDataSource primary = markedDataSource(true);
        ManagedDataSource reporting = markedDataSource(true);
        TestRoutingDataSource routing = new TestRoutingDataSource(mapOf(primary, reporting));

        assertEquals("primary", routing.currentLookupKey());
    }

    @Test
    void skipsUnhealthyDatasourceAndReturnsNextHealthy() {
        ManagedDataSource primary = markedDataSource(false);
        ManagedDataSource reporting = markedDataSource(true);
        TestRoutingDataSource routing = new TestRoutingDataSource(mapOf(primary, reporting));

        assertEquals("reporting", routing.currentLookupKey());
    }

    @Test
    void throwsWhenAllDatasourcesAreDown() {
        ManagedDataSource primary = markedDataSource(false);
        ManagedDataSource reporting = markedDataSource(false);
        TestRoutingDataSource routing = new TestRoutingDataSource(mapOf(primary, reporting));

        assertThrows(IllegalStateException.class, routing::currentLookupKey);
    }

    @Test
    void failsOverToNextMarkedHealthyDatasource() {
        ManagedDataSource primary = markedDataSource(false);
        ManagedDataSource reporting = markedDataSource(true);
        TestRoutingDataSource routing = new TestRoutingDataSource(mapOf(primary, reporting));

        assertEquals("reporting", routing.currentLookupKey());
    }

    @Test
    void retriesUntilHealthyDatasourceIsFound() throws Exception {
        ManagedDataSource primary = mock(ManagedDataSource.class);
        ManagedDataSource reporting = mock(ManagedDataSource.class);
        ManagedDataSource analytics = mock(ManagedDataSource.class);
        AtomicBoolean primaryHealthy = new AtomicBoolean(true);
        AtomicBoolean reportingHealthy = new AtomicBoolean(true);
        Connection analyticsConnection = mock(Connection.class);

        when(primary.isMarkedHealthy()).thenAnswer(i -> primaryHealthy.get());
        when(primary.getConnection()).thenAnswer(i -> {
            primaryHealthy.set(false);
            throw new SQLException("primary down");
        });

        when(reporting.isMarkedHealthy()).thenAnswer(i -> reportingHealthy.get());
        when(reporting.getConnection()).thenAnswer(i -> {
            reportingHealthy.set(false);
            throw new SQLException("reporting down");
        });

        when(analytics.isMarkedHealthy()).thenReturn(true);
        when(analytics.getConnection()).thenReturn(analyticsConnection);

        SequencedMap<String, ManagedDataSource> map = new LinkedHashMap<>();
        map.put("primary", primary);
        map.put("reporting", reporting);
        map.put("analytics", analytics);
        HealingRoutingDataSource routing = new HealingRoutingDataSource(map);
        LinkedHashMap<Object, Object> targets = new LinkedHashMap<>();
        targets.put("primary", primary);
        targets.put("reporting", reporting);
        targets.put("analytics", analytics);
        routing.setTargetDataSources(targets);
        routing.afterPropertiesSet();

        assertThrows(SQLException.class, routing::getConnection);
        assertThrows(SQLException.class, routing::getConnection);
        Connection actual = routing.getConnection();

        assertSame(analyticsConnection, actual);
    }

    @Test
    void throwsWhenAllHealthyCandidatesFail() throws Exception {
        ManagedDataSource primary = mock(ManagedDataSource.class);
        ManagedDataSource reporting = mock(ManagedDataSource.class);
        AtomicBoolean primaryHealthy = new AtomicBoolean(true);
        AtomicBoolean reportingHealthy = new AtomicBoolean(true);

        when(primary.isMarkedHealthy()).thenAnswer(i -> primaryHealthy.get());
        when(primary.getConnection()).thenAnswer(i -> {
            primaryHealthy.set(false);
            throw new SQLException("primary down");
        });

        when(reporting.isMarkedHealthy()).thenAnswer(i -> reportingHealthy.get());
        when(reporting.getConnection()).thenAnswer(i -> {
            reportingHealthy.set(false);
            throw new SQLException("reporting down");
        });

        SequencedMap<String, ManagedDataSource> map = mapOf(primary, reporting);
        HealingRoutingDataSource routing = new HealingRoutingDataSource(map);
        LinkedHashMap<Object, Object> targets = new LinkedHashMap<>();
        targets.put("primary", primary);
        targets.put("reporting", reporting);
        routing.setTargetDataSources(targets);
        routing.afterPropertiesSet();

        SQLException primaryFailure = assertThrows(SQLException.class, routing::getConnection);
        assertEquals("primary down", primaryFailure.getMessage());
        SQLException reportingFailure = assertThrows(SQLException.class, routing::getConnection);
        assertEquals("reporting down", reportingFailure.getMessage());
        SQLException exhausted = assertThrows(SQLException.class, routing::getConnection);
        assertEquals("No healthy datasource available", exhausted.getMessage());
    }

    @Test
    void getConnectionThrowsSqlExceptionWhenAllDatasourcesAreMarkedUnhealthy() {
        ManagedDataSource primary = markedDataSource(false);
        ManagedDataSource reporting = markedDataSource(false);
        HealingRoutingDataSource routing = new HealingRoutingDataSource(mapOf(primary, reporting));
        LinkedHashMap<Object, Object> targets = new LinkedHashMap<>();
        targets.put("primary", primary);
        targets.put("reporting", reporting);
        routing.setTargetDataSources(targets);
        routing.afterPropertiesSet();

        SQLException exhausted = assertThrows(SQLException.class, routing::getConnection);
        assertEquals("No healthy datasource available", exhausted.getMessage());
    }

    @Test
    void rejectsNullDatasourcesMap() {
        assertThrows(NullPointerException.class, () -> new TestRoutingDataSource(null));
    }

    @Test
    void rejectsEmptyDatasourcesMap() {
        assertThrows(IllegalArgumentException.class,
                () -> new TestRoutingDataSource(new LinkedHashMap<>()));
    }

    private static ManagedDataSource markedDataSource(boolean markedHealthy) {
        ManagedDataSource managed = mock(ManagedDataSource.class);
        when(managed.isMarkedHealthy()).thenReturn(markedHealthy);
        return managed;
    }

    private static SequencedMap<String, ManagedDataSource> mapOf(ManagedDataSource primary, ManagedDataSource reporting) {
        SequencedMap<String, ManagedDataSource> map = new LinkedHashMap<>();
        map.put("primary", primary);
        map.put("reporting", reporting);
        return map;
    }

    private static final class TestRoutingDataSource extends HealingRoutingDataSource {
        private TestRoutingDataSource(SequencedMap<String, ManagedDataSource> datasources) {
            super(datasources);
        }

        private Object currentLookupKey() {
            return determineCurrentLookupKey();
        }
    }
}
