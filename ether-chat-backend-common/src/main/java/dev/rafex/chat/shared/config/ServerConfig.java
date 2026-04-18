package dev.rafex.chat.shared.config;

import dev.rafex.ether.config.EtherConfig;

public record ServerConfig(int port, boolean sandbox, String jwtSecret, long jwtExpirySeconds) {
    public static ServerConfig from(EtherConfig config) {
        return new ServerConfig(
            config.get("SERVER_PORT").map(Integer::parseInt).orElse(8080),
            config.get("SERVER_SANDBOX").map(Boolean::parseBoolean).orElse(false),
            config.get("JWT_SECRET").orElseThrow(() -> new IllegalStateException("JWT_SECRET env var is required")),
            config.get("JWT_EXPIRY_SECONDS").map(Long::parseLong).orElse(86400L)
        );
    }
}
