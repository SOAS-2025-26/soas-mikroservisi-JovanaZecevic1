package com.soas.api_gateway.controller;

import com.soas.api_gateway.auth.BasicAuthDecoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class LoginController {

    private final WebClient webClient;

    public LoginController(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder.build();
    }

    @GetMapping("/login")
    public Mono<ResponseEntity<?>> login(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        BasicAuthDecoder.Credentials credentials = BasicAuthDecoder.decode(authHeader);
        if (credentials == null) {
            return Mono.just(unauthorized("Missing or malformed Basic Authorization header"));
        }

        return webClient.get()
                .uri("http://users-service/users/validate?email={email}&password={password}",
                        credentials.email(), credentials.password())
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> {
                    Object role = body.get("role");
                    if (role == null) {
                        return unauthorized("Invalid email or password");
                    }
                    return ResponseEntity.ok(Map.of("email", credentials.email(), "role", role.toString()));
                })
                .onErrorResume(WebClientResponseException.class, e -> Mono.just(unauthorized("Invalid email or password")))
                .onErrorResume(Exception.class, e -> Mono.just(
                        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body("Unable to validate credentials, please try again later")));
    }

    private ResponseEntity<?> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
    }

}
