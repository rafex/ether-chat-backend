package dev.rafex.chat.chat.port;
import dev.rafex.chat.chat.domain.Message;
import java.util.List;
public interface MessageRepository {
    Message save(Message message);
    List<Message> findByConversationId(String conversationId);
}
