package dev.rafex.chat.chat.handler;

import dev.rafex.chat.chat.port.ChatService;
import dev.rafex.chat.shared.config.ServerConfig;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.jwt.DefaultTokenVerifier;
import dev.rafex.ether.jwt.JwtConfig;
import dev.rafex.ether.jwt.KeyProvider;
import dev.rafex.ether.websocket.core.WebSocketCloseStatus;
import dev.rafex.ether.websocket.core.WebSocketEndpoint;
import dev.rafex.ether.websocket.core.WebSocketSession;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class WebSocketChatHandler implements WebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(WebSocketChatHandler.class.getName());
    private static final String ATTR_USER_ID = "userId";

    private final ChatService chatService;
    private final JsonCodec json;
    private final ServerConfig config;

    public WebSocketChatHandler(ChatService chatService, JsonCodec json, ServerConfig config) {
        this.chatService = Objects.requireNonNull(chatService, "chatService");
        this.json = Objects.requireNonNull(json, "json");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public void onOpen(WebSocketSession session) {
        String authHeader = session.headerFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length()).trim();
            String userId = verifyToken(token);
            if (userId != null) {
                session.attribute(ATTR_USER_ID, userId);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onText(WebSocketSession session, String text) throws Exception {
        var body = json.readValue(text, Map.class);

        String userId = (String) session.attribute(ATTR_USER_ID);
        if (userId == null) {
            String token = body != null ? (String) body.get("token") : null;
            if (token != null && !token.isBlank()) {
                userId = verifyToken(token);
            }
            if (userId == null) {
                session.close(WebSocketCloseStatus.of(1008, "Unauthorized")).toCompletableFuture().join();
                return;
            }
            session.attribute(ATTR_USER_ID, userId);
        }

        String message = body != null ? (String) body.get("message") : null;
        if (message == null || message.isBlank()) {
            session.sendText(json.toJson(Map.of("error", "message is required")));
            return;
        }

        String conversationId = body != null ? (String) body.get("conversation_id") : null;

        try {
            var chatResponse = chatService.sendMessage(userId, conversationId, message);
            session.sendText(json.toJson(Map.of(
                "content", chatResponse.content(),
                "conversation_id", chatResponse.conversationId()
            )));
        } catch (Exception e) {
            LOG.warning("WS chat error for user " + userId + ": " + e.getMessage());
            session.sendText(json.toJson(Map.of("error", "internal server error")));
        }
    }

    @Override
    public void onError(WebSocketSession session, Throwable error) {
        LOG.warning("WS error on session " + session.id() + ": " + error.getMessage());
    }

    private String verifyToken(String token) {
        try {
            var kp = KeyProvider.hmac(config.jwtSecret());
            var jc = JwtConfig.builder(kp).requireExpiration(false).requireSubject(true).build();
            var result = new DefaultTokenVerifier(jc).verify(token, Instant.now());
            return result.ok() ? result.claims().get().subject() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
