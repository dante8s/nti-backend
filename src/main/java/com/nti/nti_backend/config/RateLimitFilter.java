package com.nti.nti_backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(-200)
public class RateLimitFilter extends OncePerRequestFilter {

    // key = "path:ip" → Bucket
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        int capacity = capacityFor(path);

        if (capacity == 0) {
            chain.doFilter(request, response);
            return;
        }

        String ip = extractIp(request);
        Bucket bucket = buckets.computeIfAbsent(path + ":" + ip, k -> newBucket(capacity));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Забагато спроб. Спробуйте пізніше.\"}");
        }
    }

    private int capacityFor(String path) {
        if (path.startsWith("/api/auth/login"))           return 10; // 10 спроб/хв на IP
        if (path.startsWith("/api/auth/register"))        return 5;  // 5 реєстрацій/хв на IP
        if (path.startsWith("/api/auth/forgot-password")) return 3;  // 3 запити/хв на IP
        return 0;
    }

    private Bucket newBucket(int capacity) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
