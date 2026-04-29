package dev.rafex.chat.auth.infra;

import dev.rafex.chat.auth.domain.User;
import dev.rafex.ether.jdbc.client.JdbcDatabaseClient;
import dev.rafex.ether.jdbc.datasource.SimpleDataSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryImplTest {

    @Test
    void savesAndFindsUserByUsername() {
        var repository = newRepository();

        repository.save(new User(null, "demo", "hashed-password", null));
        var found = repository.findByUsername("demo");

        assertTrue(found.isPresent());
        assertEquals("demo", found.get().username());
        assertEquals("hashed-password", found.get().passwordHash());
    }

    private static UserRepositoryImpl newRepository() {
        var db = new JdbcDatabaseClient(new SimpleDataSource("jdbc:sqlite:" + temporaryDatabasePath()));
        AuthDb.init(db);
        return new UserRepositoryImpl(db);
    }

    private static String temporaryDatabasePath() {
        try {
            var file = Files.createTempFile("user-repository-", ".db");
            file.toFile().deleteOnExit();
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new IllegalStateException("failed to create temporary sqlite database", e);
        }
    }
}
