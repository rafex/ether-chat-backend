package dev.rafex.chat.shared.config;

import dev.rafex.ether.config.EtherConfig;
import dev.rafex.ether.config.sources.EnvironmentConfigSource;
import dev.rafex.ether.config.sources.SystemPropertyConfigSource;
import java.util.Objects;

public record AppConfig(DatabaseConfig database, ServerConfig server, AiConfig ai) {
    public AppConfig {
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(ai, "ai");
    }
    private static volatile AppConfig INSTANCE;
    public static AppConfig load() {
        if (INSTANCE == null) {
            synchronized (AppConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = loadFrom(EtherConfig.of(new EnvironmentConfigSource(), new SystemPropertyConfigSource()));
                }
            }
        }
        return INSTANCE;
    }
    public static AppConfig loadFrom(EtherConfig config) {
        return new AppConfig(DatabaseConfig.from(config), ServerConfig.from(config), AiConfig.from(config));
    }
    public static void reset() { synchronized (AppConfig.class) { INSTANCE = null; } }
}
