package dev.rafex.chat.chat.infra;

import dev.rafex.chat.chat.domain.Message;
import dev.rafex.chat.chat.port.AiGateway;
import java.util.List;

public final class EchoAiGateway implements AiGateway {
    @Override
    public String generate(List<Message> history) {
        if (history == null || history.isEmpty()) return "[ECHO] (empty history)";
        return "[ECHO] " + history.get(history.size() - 1).content();
    }
}
