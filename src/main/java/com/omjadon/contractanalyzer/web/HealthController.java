package com.omjadon.contractanalyzer.web;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal, anonymous readiness probe. Never exposes database or AI credentials. */
@RestController
public class HealthController {
    private final ObjectProvider<DataSource> dataSources;

    public HealthController(ObjectProvider<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        DataSource dataSource = dataSources.getIfAvailable();
        if (dataSource == null) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("status", "UP", "database", "NOT_CONFIGURED"));
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(3);
            statement.execute("SELECT 1");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("status", "UP", "database", "UP"));
        } catch (Exception ignored) {
            // Do not disclose connection strings, database names or SQL details.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("status", "DOWN", "database", "DOWN"));
        }
    }
}
