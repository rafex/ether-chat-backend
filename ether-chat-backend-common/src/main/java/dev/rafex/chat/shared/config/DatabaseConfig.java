package dev.rafex.chat.shared.config;

import dev.rafex.ether.config.EtherConfig;

public record DatabaseConfig(String authDbPath, String chatDbPath) {
    public static DatabaseConfig from(EtherConfig config) {
        return new DatabaseConfig(
            config.get("AUTH_DB_PATH").orElse("./data/auth.db"),
            config.get("CHAT_DB_PATH").orElse("./data/chat.db")
        );
    }
}
