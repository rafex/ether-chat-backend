package dev.rafex.chat.chat.handler;

import dev.rafex.chat.chat.domain.ChatResponse;
import dev.rafex.chat.chat.port.ChatService;
import dev.rafex.chat.shared.config.AppConfig;
import dev.rafex.ether.config.EtherConfig;
import dev.rafex.ether.config.sources.MapConfigSource;
import dev.rafex.ether.json.JsonCodecBuilder;
import dev.rafex.ether.websocket.core.WebSocketCloseStatus;
import dev.rafex.ether.websocket.core.WebSocketSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketChatHandlerTest {

    private ChatService chatService;
    private WebSocketChatHandler handler;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        var json = JsonCodecBuilder.lenient().build();
        var config = AppConfig.loadFrom(EtherConfig.of(new MapConfigSource("test", Map.of(
            "JWT_SECRET", "test-secret-minimum-32-characters-x",
            "WS_PORT", "8081"
        ))));
        handler = new WebSocketChatHandler(chatService, json, config.server());
    }

    @Test
    void closesSessionWhenNoAuthProvided() throws Exception {
        var session = new RecordingSession(null);

        handler.onText(session, "{\"message\":\"Hello\"}");

        assertNotNull(session.closedWith, "session should be closed");
        assertEquals(0, session.sentTexts.size(), "no text should be sent");
    }

    @Test
    void sendsErrorWhenMessageIsBlank() throws Exception {
        var session = new RecordingSession("user-1");

        handler.onText(session, "{\"message\":\"\"}");

        assertEquals(1, session.sentTexts.size());
        assertTrue(session.sentTexts.get(0).contains("error"));
    }

    @Test
    void callsChatServiceAndSendsResponseWhenAuthenticated() throws Exception {
        var session = new RecordingSession("user-1");
        when(chatService.sendMessage(eq("user-1"), any(), eq("Hello")))
            .thenReturn(new ChatResponse("Hi!", "conv-123"));

        handler.onText(session, "{\"message\":\"Hello\",\"conversation_id\":null}");

        verify(chatService).sendMessage("user-1", null, "Hello");
        assertEquals(1, session.sentTexts.size());
        assertTrue(session.sentTexts.get(0).contains("Hi!"));
    }

    @Test
    void rejectsInvalidTokenInMessageBody() throws Exception {
        var session = new RecordingSession(null);

        handler.onText(session, "{\"token\":\"invalid.jwt.value\",\"message\":\"Hello\"}");

        assertNotNull(session.closedWith, "session should be closed on invalid token");
        verify(chatService, never()).sendMessage(any(), any(), any());
    }

    @Test
    void onOpenDoesNotCloseSessionWhenNoAuthHeader() {
        var session = new RecordingSession(null);
        session.headers.remove("Authorization");

        handler.onOpen(session);

        assertEquals(null, session.closedWith, "onOpen must not close when no header");
    }

    @Test
    void onOpenDoesNotCloseSessionOnInvalidBearerToken() {
        var session = new RecordingSession(null);
        session.headers.put("Authorization", "Bearer invalid.token");

        handler.onOpen(session);

        assertEquals(null, session.closedWith, "onOpen must not close — rejection happens in onText");
    }

    @Test
    void sendsErrorJsonOnChatServiceException() throws Exception {
        var session = new RecordingSession("user-1");
        when(chatService.sendMessage(any(), any(), any()))
            .thenThrow(new RuntimeException("DB error"));

        handler.onText(session, "{\"message\":\"Hello\"}");

        assertEquals(1, session.sentTexts.size());
        assertTrue(session.sentTexts.get(0).contains("error"));
    }

    private static final class RecordingSession implements WebSocketSession {
        final Map<String, String> attributes = new HashMap<>();
        final Map<String, String> headers = new HashMap<>();
        final List<String> sentTexts = new ArrayList<>();
        WebSocketCloseStatus closedWith = null;

        RecordingSession(String userId) {
            if (userId != null) attributes.put("userId", userId);
        }

        @Override public String id() { return "test-session"; }
        @Override public String path() { return "/ws/chat"; }
        @Override public String subprotocol() { return null; }
        @Override public boolean isOpen() { return closedWith == null; }
        @Override public String pathParam(String name) { return null; }
        @Override public String queryFirst(String name) { return null; }
        @Override public List<String> queryAll(String name) { return List.of(); }
        @Override public String headerFirst(String name) { return headers.get(name); }
        @Override public Object attribute(String name) { return attributes.get(name); }
        @Override public void attribute(String name, Object value) { attributes.put(name, value != null ? value.toString() : null); }
        @Override public Map<String, String> pathParams() { return Map.of(); }
        @Override public Map<String, List<String>> queryParams() { return Map.of(); }
        @Override public Map<String, List<String>> headers() { return Map.of(); }
        @Override
        public CompletionStage<Void> sendText(String text) {
            sentTexts.add(text);
            return CompletableFuture.completedFuture(null);
        }
        @Override
        public CompletionStage<Void> sendBinary(ByteBuffer data) {
            return CompletableFuture.completedFuture(null);
        }
        @Override
        public CompletionStage<Void> close(WebSocketCloseStatus status) {
            this.closedWith = status;
            return CompletableFuture.completedFuture(null);
        }
    }
}
