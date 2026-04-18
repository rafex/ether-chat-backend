package dev.rafex.chat.chat.port;
import dev.rafex.chat.chat.domain.Conversation;
import java.util.Optional;
public interface ConversationRepository {
    Optional<Conversation> findById(String id);
    Conversation save(Conversation conversation);
    Conversation findOrCreate(String userId, String conversationId);
}
