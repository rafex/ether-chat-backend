package dev.rafex.chat.bootstrap;

import dev.rafex.chat.auth.port.AuthPort;
import dev.rafex.chat.chat.port.ChatService;
import dev.rafex.ether.json.JsonCodec;
import java.util.Objects;

public record AppContainer(AuthPort authPort, ChatService chatService, JsonCodec json) {
    public AppContainer {
        Objects.requireNonNull(authPort, "authPort");
        Objects.requireNonNull(chatService, "chatService");
        Objects.requireNonNull(json, "json");
    }
}
