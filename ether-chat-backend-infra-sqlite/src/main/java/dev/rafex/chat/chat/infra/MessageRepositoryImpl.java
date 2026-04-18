package dev.rafex.chat.chat.infra;

import dev.rafex.chat.chat.domain.Message;
import dev.rafex.chat.chat.domain.MessageRole;
import dev.rafex.chat.chat.port.MessageRepository;
import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.sql.SqlBuilder;
import dev.rafex.ether.database.core.sql.SqlQuery;
import java.util.List;
import java.util.Objects;

public final class MessageRepositoryImpl implements MessageRepository {
    private final DatabaseClient db;

    public MessageRepositoryImpl(DatabaseClient db) {
        this.db = Objects.requireNonNull(db, "db");
    }

    @Override
    public Message save(Message msg) {
        db.execute(new SqlBuilder("INSERT INTO messages(conversation_id, role, content) VALUES(?,?,?)")
            .param(msg.conversationId()).param(msg.role().name()).param(msg.content()).build());
        long id = db.queryOne(SqlQuery.of("SELECT last_insert_rowid()"), rs -> rs.getLong(1)).orElse(0L);
        return new Message(id, msg.conversationId(), msg.role(), msg.content(), null);
    }

    @Override
    public List<Message> findByConversationId(String convId) {
        return db.queryList(
            new SqlBuilder("SELECT id, conversation_id, role, content, created_at FROM messages WHERE conversation_id=? ORDER BY created_at ASC").param(convId).build(),
            rs -> new Message(rs.getLong("id"), rs.getString("conversation_id"), MessageRole.valueOf(rs.getString("role")), rs.getString("content"), rs.getString("created_at"))
        );
    }
}
