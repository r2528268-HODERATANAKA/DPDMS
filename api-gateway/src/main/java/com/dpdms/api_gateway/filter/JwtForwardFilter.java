package com.dpdms.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Runs before EVERY request that passes through the gateway.
//
//   1. /api/auth/login is allowed through with no token (you need it to GET a token).
//   2. Every other /api/** request must carry  Authorization: Bearer <jwt>.
//   3. The token is verified, and its claims are forwarded downstream as headers:
//         X-User-Name  (full name -> becomes reviewedBy on approvals)
//         X-User-Role  (WARD_RECORDER / PROVINCIAL_SUPERVISOR / PROVINCIAL_ADMIN)
//         X-User-Ward  (recorders only, may be absent)
//         X-User-Hazard(flood / drought / fire / zoonotic / mining)
//      This is exactly what the hazard services read (see their controllers) —
//      the leader's flood-service NOTE anticipated this swap.
//   4. SECURITY: any X-User-* headers the caller sent themselves are REMOVED first,
//      so nobody can fake their identity by adding a header.
//   5. No/invalid token -> HTTP 401, request never reaches a hazard service.
@Component
public class JwtForwardFilter implements GlobalFilter, Ordered {

    private final JwtChecker jwtChecker;

    public JwtForwardFilter(JwtChecker jwtChecker) {
        this.jwtChecker = jwtChecker;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Only guard our API; login needs no token yet
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/login")) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        Claims claims;
        try {
            claims = jwtChecker.parse(authorization.substring(7).trim());
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorized(exchange);
        }

        // Strip caller-supplied identity headers, then add the real ones from the token
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Name");
                    headers.remove("X-User-Role");
                    headers.remove("X-User-Ward");
                    headers.remove("X-User-Hazard");
                })
                .build();

        if (claims.get("name", String.class) != null) {
            request = request.mutate().header("X-User-Name", claims.get("name", String.class)).build();
        }
        if (claims.get("role", String.class) != null) {
            request = request.mutate().header("X-User-Role", claims.get("role", String.class)).build();
        }
        if (claims.get("ward", String.class) != null) {
            request = request.mutate().header("X-User-Ward", claims.get("ward", String.class)).build();
        }
        if (claims.get("hazard", String.class) != null) {
            request = request.mutate().header("X-User-Hazard", claims.get("hazard", String.class)).build();
        }

        return chain.filter(exchange.mutate().request(request).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"status\":401,\"message\":\"Missing, invalid or expired token\"}"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    // Negative order = run early, before the routing filters
    @Override
    public int getOrder() {
        return -100;
    }
}
