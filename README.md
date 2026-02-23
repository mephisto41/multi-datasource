# multi-datasource

Dynamic multi-datasource routing for Spring Boot with:
- ordered failover across named datasources
- unhealthy marking on connection-borrow failure
- on-demand heal/replacement when no datasource is currently available

## How It Works

1. `HealingRoutingDataSource` selects the first datasource marked healthy (in configured order).
2. `ManagedDataSource` marks itself unhealthy when `getConnection(...)` fails.
3. Caller retries the operation; the next retry routes to the next healthy datasource.
4. If none are marked healthy, routing attempts to heal all datasources once and retries.

Notes:
- Routing exhaustion is surfaced as `SQLException("No healthy datasource available")`.
- Failover is caller-driven for borrow failures; routing has one internal heal-and-retry only when none are healthy.

## Configuration

Configure datasources under `app.datasources`:

```yaml
app:
  datasources:
    primary:
      url: jdbc:postgresql://localhost:5432/app
      username: app
      password: app
      driver-class-name: org.postgresql.Driver
      validation-query: SELECT 1
      maximum-pool-size: 10
    reporting:
      url: jdbc:mysql://localhost:3306/reporting
      username: report
      password: report
      driver-class-name: com.mysql.cj.jdbc.Driver
      validation-query: SELECT 1
      maximum-pool-size: 5
    analytics:
      url: jdbc:sqlserver://localhost:1433;databaseName=analytics;encrypt=false
      username: analytics
      password: analytics
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
      validation-query: SELECT 1
      maximum-pool-size: 8
```

The first configured datasource has highest priority.

## Build & Test

```bash
mvn test
```

## Usage Pattern (Caller Retry)

Use application-level retry around the full unit of work (not just `getConnection()`):

```java
public <T> T withRetry(Callable<T> action, int maxAttempts) throws Exception {
    SQLException lastSqlException = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return action.call();
        } catch (SQLException ex) {
            lastSqlException = ex;
            if (attempt == maxAttempts) {
                throw ex;
            }
        }
    }
    throw lastSqlException;
}
```

Why this pattern:
- if current datasource fails on borrow, it is marked unhealthy
- next retry attempts route to the next healthy datasource
- when all are unavailable, you get `SQLException("No healthy datasource available")`

## Key Classes

- `src/main/java/com/example/multids/config/DynamicDatasourceConfiguration.java`
- `src/main/java/com/example/multids/routing/HealingRoutingDataSource.java`
- `src/main/java/com/example/multids/datasource/ManagedDataSource.java`

## Integration Test

`src/test/java/com/example/multids/routing/HealingRoutingDataSourceIntegrationTest.java` verifies:
- all 3 datasources can query normally
- failover from `primary` to next datasource when `primary` is unavailable
- successful routing back to `primary` after heal
