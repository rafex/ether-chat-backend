package dev.rafex.chat.bootstrap;

import dev.rafex.chat.auth.domain.User;
import dev.rafex.chat.auth.infra.PasswordHelper;
import dev.rafex.chat.auth.port.UserRepository;
import java.util.logging.Logger;

public final class SeedUsers {
    private static final Logger LOG = Logger.getLogger(SeedUsers.class.getName());
    private SeedUsers() {}

    public static void seedIfAbsent(UserRepository repo) {
        if (repo.findByUsername("demo").isEmpty()) {
            var hash = PasswordHelper.hash("password123");
            repo.save(new User(null, "demo", hash, null));
            LOG.info("Seeded demo user");
        }
    }
}
