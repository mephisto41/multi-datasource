package com.example.multids.datasource.health;

import com.example.multids.datasource.ManagedDataSource;
import com.example.multids.datasource.MultiDataSourceRegistry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DataSourceAutoHealerTest {

    @Test
    void callsHealForEveryDatasource() {
        ManagedDataSource one = Mockito.mock(ManagedDataSource.class);
        ManagedDataSource two = Mockito.mock(ManagedDataSource.class);
        MultiDataSourceRegistry registry = new MultiDataSourceRegistry(Map.of("one", one, "two", two));

        DataSourceAutoHealer healer = new DataSourceAutoHealer(registry);
        healer.healAll();

        verify(one, times(1)).healIfNeeded();
        verify(two, times(1)).healIfNeeded();
    }

    @Test
    void continuesWhenOneDatasourceThrows() {
        ManagedDataSource one = Mockito.mock(ManagedDataSource.class);
        ManagedDataSource two = Mockito.mock(ManagedDataSource.class);
        doThrow(new RuntimeException("boom")).when(one).healIfNeeded();
        MultiDataSourceRegistry registry = new MultiDataSourceRegistry(Map.of("one", one, "two", two));

        DataSourceAutoHealer healer = new DataSourceAutoHealer(registry);
        assertDoesNotThrow(healer::healAll);

        verify(one, times(1)).healIfNeeded();
        verify(two, times(1)).healIfNeeded();
    }
}
