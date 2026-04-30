package dev.rafex.chat.auth.service;

import dev.rafex.chat.auth.domain.Credentials;
import dev.rafex.chat.auth.domain.Session;
import dev.rafex.chat.auth.port.AuthPort;
import dev.rafex.chat.auth.port.UserRepository;
import dev.rafex.chat.shared.config.ServerConfig;
import dev.rafex.chat.shared.error.AppError;
import dev.rafex.ether.crypto.password.PasswordHasherPBKDF2;
import dev.rafex.ether.jwt.DefaultTokenIssuer;
import dev.rafex.ether.jwt.JwtConfig;
import dev.rafex.ether.jwt.KeyProvider;
import dev.rafex.ether.jwt.TokenSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.logging.Logger;

public final class AuthServiceImpl implements AuthPort {
    private static final Logger LOG = Logger.getLogger(AuthServiceImpl.class.getName());
    private final UserRepository repository;
    private final ServerConfig config;

    public AuthServiceImpl(UserRepository repository, ServerConfig config) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Session login(Credentials credentials) {
        if (credentials == null || credentials.username() == null || credentials.username().isBlank()
                || credentials.password() == null || credentials.password().isBlank()) {
            throw new AppError.BadRequest("username and password are required");
        }
        var user = repository.findByUsername(credentials.username())
            .orElseThrow(() -> new AppError.Unauthorized("Invalid credentials"));
        if (!verifyPassword(credentials.password(), user.passwordHash())) {
            throw new AppError.Unauthorized("Invalid credentials");
        }
        var kp = KeyProvider.hmac(config.jwtSecret());
        var jc = JwtConfig.builder(kp).requireExpiration(false).requireSubject(true).build();
        var spec = TokenSpec.builder()
            .subject(user.username())
            .ttl(Duration.ofSeconds(config.jwtExpirySeconds()))
            .build();
        var token = new DefaultTokenIssuer(jc).issue(spec);
        var expiresAt = Instant.now().plusSeconds(config.jwtExpirySeconds()).toString();
        return new Session(token, expiresAt, user.username());
    }

    public static boolean verifyPassword(String password, String stored) {
        try {
            String[] parts = stored.split(":");
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            int iterations = Integer.parseInt(parts[1]);
            byte[] hash = Base64.getDecoder().decode(parts[2]);
            return new PasswordHasherPBKDF2(iterations).verify(password.toCharArray(), salt, iterations, hash);
        } catch (Exception e) {
            LOG.warning("Error verifying password: " + e.getMessage());
            return false;
        }
    }
}
