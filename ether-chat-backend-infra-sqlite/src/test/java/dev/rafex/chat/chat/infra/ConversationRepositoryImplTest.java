package dev.rafex.chat.chat.infra;

import dev.rafex.chat.chat.domain.Conversation;
import dev.rafex.chat.shared.error.AppError;
import dev.rafex.ether.jdbc.client.JdbcDatabaseClient;
import dev.rafex.ether.jdbc.datasource.SimpleDataSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversationRepositoryImplTest {

    @Test
    void reusesConversationOnlyForSameUser() {
        var repository = newRepository();
        repository.save(new Conversation("conv-1", "user-a", null, null));

        var reused = repository.findOrCreate("user-a", "conv-1");

        assertEquals("conv-1", reused.id());
        assertEquals("user-a", reused.userId());
    }

    @Test
    void rejectsConversationOwnedByDifferentUser() {
        var repository = newRepository();
        repository.save(new Conversation("conv-1", "user-a", null, null));

        var error = assertThrows(AppError.Unauthorized.class, () -> repository.findOrCreate("user-b", "conv-1"));

        assertEquals("conversation does not belong to the authenticated user", error.getMessage());
    }

    private static ConversationRepositoryImpl newRepository() {
        var db = new JdbcDatabaseClient(new SimpleDataSource("jdbc:sqlite:" + temporaryDatabasePath()));
        ChatDb.init(db);
        return new ConversationRepositoryImpl(db);
    }

    private static String temporaryDatabasePath() {
        try {
            var file = Files.createTempFile("conversation-repository-", ".db");
            file.toFile().deleteOnExit();
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new IllegalStateException("failed to create temporary sqlite database", e);
        }
    }
}
