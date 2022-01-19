package com.example.apigatewayservice.filter;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    private final Environment env;

    public AuthorizationHeaderFilter(final Environment env) {
        super(Config.class);
        this.env = env;
    }

    @Override
    public GatewayFilter apply(final Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (!request.getHeaders()
                        .containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }
            String authorizationHeader = request.getHeaders()
                                                .get(HttpHeaders.AUTHORIZATION)
                                                .get(0);
            String jwt = authorizationHeader.replace("Bearer", "");
            if (!isJwtValid(jwt)) {
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }
            return chain.filter(exchange);
        });
    }
    private Mono<Void> onError(final ServerWebExchange exchange, final String error, final HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(error);
        return response.setComplete();
    }

    private boolean isJwtValid(final String jwt) {
        boolean returnValue = true;
        String subject = null;
        try {
            subject = Jwts.parser()
                          .setSigningKey(env.getProperty("token.secret"))
                          .parseClaimsJws(jwt)
                          .getBody()
                          .getSubject();
        } catch (Exception ex) {
            returnValue = false;
        }
        if (subject == null || subject.isEmpty()) {
            returnValue = false;
        }
        return returnValue;
    }

    public static class Config {

    }

    @Bean
    public HttpTraceRepository httpTraceRepository() {
        return new InMemoryHttpTraceRepository();
    }
}
