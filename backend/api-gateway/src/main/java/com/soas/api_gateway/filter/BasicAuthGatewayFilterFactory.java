package com.soas.api_gateway.filter;

import com.soas.api_gateway.auth.BasicAuthDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class BasicAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final Logger log = LoggerFactory.getLogger(BasicAuthGatewayFilterFactory.class);

    private final WebClient webClient;

    public BasicAuthGatewayFilterFactory(WebClient.Builder loadBalancedWebClientBuilder) {
        super(Object.class);
        this.webClient = loadBalancedWebClientBuilder.build();
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            BasicAuthDecoder.Credentials credentials = BasicAuthDecoder.decode(authHeader);
            if (credentials == null) {
                return unauthorized(exchange, "Missing or malformed Basic Authorization header");
            }

            String finalEmail = credentials.email();
            return webClient.get()
                    .uri("http://users-service/users/validate?email={email}&password={password}",
                            credentials.email(), credentials.password())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .flatMap(body -> {
                        Object role = body.get("role");
                        if (role == null) {
                            return unauthorized(exchange, "Invalid email or password");
                        }
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-Actor-Role", role.toString())
                                .header("X-Actor-Email", finalEmail)
                                .build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    })
                    .onErrorResume(WebClientResponseException.Unauthorized.class,
                            e -> unauthorized(exchange, "Invalid email or password"))
                    .onErrorResume(WebClientResponseException.class,
                            e -> unauthorized(exchange, "Invalid email or password"))
                    .onErrorResume(Exception.class,
                            e -> {
                                log.error("Credential validation call to users-service failed", e);
                                return unauthorized(exchange, "Unable to validate credentials, please try again later");
                            });
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"soas\"");
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

}
