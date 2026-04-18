package dev.rafex.chat.chat.handler;

import dev.rafex.chat.chat.port.ChatService;
import dev.rafex.chat.shared.config.ServerConfig;
import dev.rafex.chat.shared.error.AppError;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.jwt.DefaultTokenVerifier;
import dev.rafex.ether.jwt.JwtConfig;
import dev.rafex.ether.jwt.KeyProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class ChatMessageHandler extends Handler.Abstract.NonBlocking {
    private static final Logger LOG = Logger.getLogger(ChatMessageHandler.class.getName());
    private final ChatService chatService;
    private final JsonCodec json;
    private final ServerConfig config;

    public ChatMessageHandler(ChatService chatService, JsonCodec json, ServerConfig config) {
        this.chatService = Objects.requireNonNull(chatService, "chatService");
        this.json = Objects.requireNonNull(json, "json");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(405); callback.succeeded(); return true;
        }
        try {
            String authHeader = request.getHeaders().get("Authorization");
            if (authHeader == null || authHeader.isBlank()) {
                return error(response, callback, 401, "Authorization header is required");
            }
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            var kp = KeyProvider.hmac(config.jwtSecret());
            var jc = JwtConfig.builder(kp).requireExpiration(false).requireSubject(true).build();
            var result = new DefaultTokenVerifier(jc).verify(token, Instant.now());
            if (!result.ok()) {
                return error(response, callback, 401, "Token invalid or expired");
            }
            String userId = result.claims().get().subject();

            InputStream is = Request.asInputStream(request);
            var body = json.readValue(is, Map.class);
            var message = body != null ? (String) body.get("message") : null;
            var conversationId = body != null ? (String) body.get("conversation_id") : null;
            if (message == null || message.isBlank()) {
                return error(response, callback, 400, "message is required");
            }
            var chatResponse = chatService.sendMessage(userId, conversationId, message);
            byte[] bytes = json.toJsonBytes(Map.of("content", chatResponse.content(), "conversation_id", chatResponse.conversationId()));
            response.setStatus(200);
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json");
            response.write(true, ByteBuffer.wrap(bytes), callback);
            return true;
        } catch (AppError.BadRequest e) {
            return error(response, callback, 400, e.getMessage());
        } catch (AppError.Unauthorized e) {
            return error(response, callback, 401, e.getMessage());
        } catch (Exception e) {
            LOG.warning("Chat error: " + e.getMessage());
            return error(response, callback, 500, "Internal server error");
        }
    }

    private boolean error(Response response, Callback callback, int status, String detail) {
        byte[] bytes = json.toJsonBytes(Map.of("status", status, "title", status == 400 ? "Bad Request" : status == 401 ? "Unauthorized" : "Error", "detail", detail));
        response.setStatus(status);
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/problem+json");
        response.write(true, ByteBuffer.wrap(bytes), callback);
        return true;
    }
}
