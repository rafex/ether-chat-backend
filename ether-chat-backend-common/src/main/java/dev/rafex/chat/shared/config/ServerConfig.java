package dev.rafex.chat.shared.config;

import dev.rafex.ether.config.EtherConfig;

public record ServerConfig(int port, int wsPort, boolean sandbox, String jwtSecret, long jwtExpirySeconds) {
    public static ServerConfig from(EtherConfig config) {
        int port = config.get("SERVER_PORT").map(Integer::parseInt).orElse(8080);
        return new ServerConfig(
            port,
            config.get("WS_PORT").map(Integer::parseInt).orElse(port + 1),
            config.get("SERVER_SANDBOX").map(Boolean::parseBoolean).orElse(false),
            config.get("JWT_SECRET").orElseThrow(() -> new IllegalStateException("JWT_SECRET env var is required")),
            config.get("JWT_EXPIRY_SECONDS").map(Long::parseLong).orElse(86400L)
        );
    }
}
