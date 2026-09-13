package com.bitly.urlshortner.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight health-check endpoint used by Render (and any other uptime monitor).
 *
 * GET /healthz
 *   → 200 OK   when both PostgreSQL and Redis are reachable
 *   → 503 Service Unavailable  when either dependency is down
 *
 * Response body:
 * {
 *   "status"    : "UP" | "DOWN",
 *   "timestamp" : <epoch-millis>,
 *   "checks": {
 *     "database" : "UP" | "DOWN",
 *     "redis"    : "UP" | "DOWN"
 *   }
 * }
 */
@RestController
@RequestMapping("/healthz")
public class HealthController {

    private final JdbcTemplate    jdbcTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate,
                            RedisTemplate<String, String> redisTemplate) {
        this.jdbcTemplate  = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {

        String dbStatus    = checkDatabase();
        String redisStatus = checkRedis();

        boolean allUp = "UP".equals(dbStatus) && "UP".equals(redisStatus);

        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("database", dbStatus);
        checks.put("redis",    redisStatus);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    allUp ? "UP" : "DOWN");
        body.put("timestamp", Instant.now().toEpochMilli());
        body.put("checks",    checks);

        HttpStatus httpStatus = allUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(body);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkRedis() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
