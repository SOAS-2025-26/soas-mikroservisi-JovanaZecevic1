package com.soas.api_gateway.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthDecoder {

    public record Credentials(String email, String password) {
    }

    public static Credentials decode(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return null;
        }
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(authHeader.substring("Basic ".length()).trim()),
                    StandardCharsets.UTF_8);
            int separatorIndex = decoded.indexOf(':');
            if (separatorIndex < 0) {
                return null;
            }
            return new Credentials(decoded.substring(0, separatorIndex), decoded.substring(separatorIndex + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
