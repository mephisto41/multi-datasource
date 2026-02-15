package com.example.multids;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class MultiDatasourceApplicationTest {

    @Test
    void constructorIsCovered() {
        assertNotNull(new MultiDatasourceApplication());
    }

    @Test
    void mainStartsApplicationEntryPoint() {
        try (MockedStatic<SpringApplication> mockedSpringApplication = Mockito.mockStatic(SpringApplication.class)) {
            MultiDatasourceApplication.main(new String[] {"--spring.main.web-application-type=none"});
            mockedSpringApplication.verify(
                    () -> SpringApplication.run(any(Class.class), any(String[].class)),
                    times(1)
            );
        }
    }
}
