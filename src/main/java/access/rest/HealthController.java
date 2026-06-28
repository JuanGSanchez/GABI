package access.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Minimal health-check endpoint for the GABI server mode.
 *
 * <p>{@code GET /health} returns a JSON object with status {@code "UP"} and the
 * server timestamp. This is intentionally lightweight — it does NOT probe the Derby
 * DataSource or the Ollama endpoint to avoid leaking connectivity details. Use Spring
 * Boot Actuator if deeper health checks are needed.
 *
 * @author JuanGS (workstream c — access layer)
 */
@RestController
public class HealthController {

    /**
     * {@code GET /health}
     *
     * <p>Response example:
     * <pre>{"status":"UP","timestamp":"2026-06-12T10:00:00Z"}</pre>
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
    }
}
