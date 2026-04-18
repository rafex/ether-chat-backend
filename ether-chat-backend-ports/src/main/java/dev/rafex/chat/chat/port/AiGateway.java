package dev.rafex.chat.chat.port;
import dev.rafex.chat.chat.domain.Message;
import java.util.List;
public interface AiGateway {
    String generate(List<Message> history);
}
