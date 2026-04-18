package dev.rafex.chat.chat.infra;

import dev.rafex.chat.chat.domain.Conversation;
import dev.rafex.chat.chat.port.ConversationRepository;
import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ConversationRepositoryImpl implements ConversationRepository {
    private final DatabaseClient db;

    public ConversationRepositoryImpl(DatabaseClient db) {
        this.db = Objects.requireNonNull(db, "db");
    }

    @Override
    public Optional<Conversation> findById(String id) {
        return db.queryOne(
            new SqlBuilder("SELECT id, user_id, created_at, updated_at FROM conversations WHERE id=?").param(id).build(),
            rs -> new Conversation(rs.getString("id"), rs.getString("user_id"), rs.getString("created_at"), rs.getString("updated_at"))
        );
    }

    @Override
    public Conversation save(Conversation conv) {
        db.execute(new SqlBuilder("INSERT INTO conversations(id, user_id) VALUES(?,?)").param(conv.id()).param(conv.userId()).build());
        return findById(conv.id()).orElseThrow();
    }

    @Override
    public Conversation findOrCreate(String userId, String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            var existing = findById(conversationId);
            if (existing.isPresent()) return existing.get();
        }
        var id = UUID.randomUUID().toString();
        return save(new Conversation(id, userId, null, null));
    }
}
