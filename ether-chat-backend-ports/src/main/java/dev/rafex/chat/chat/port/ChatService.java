package dev.rafex.chat.chat.port;
import dev.rafex.chat.chat.domain.ChatResponse;
public interface ChatService {
    ChatResponse sendMessage(String userId, String conversationId, String message);
}
