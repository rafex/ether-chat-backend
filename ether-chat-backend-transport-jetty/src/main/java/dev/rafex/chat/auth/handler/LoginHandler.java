package dev.rafex.chat.auth.handler;

import dev.rafex.chat.auth.domain.Credentials;
import dev.rafex.chat.auth.port.AuthPort;
import dev.rafex.chat.shared.config.ServerConfig;
import dev.rafex.chat.shared.error.AppError;
import dev.rafex.ether.json.JsonCodec;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class LoginHandler extends Handler.Abstract.NonBlocking {
    private static final Logger LOG = Logger.getLogger(LoginHandler.class.getName());
    private final AuthPort authPort;
    private final JsonCodec json;
    private final ServerConfig config;

    public LoginHandler(AuthPort authPort, JsonCodec json, ServerConfig config) {
        this.authPort = Objects.requireNonNull(authPort, "authPort");
        this.json = Objects.requireNonNull(json, "json");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        applyCors(response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(204);
            callback.succeeded();
            return true;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(405);
            callback.succeeded();
            return true;
        }
        try {
            InputStream is = Request.asInputStream(request);
            var credentials = json.readValue(is, Credentials.class);
            if (credentials == null || credentials.username() == null || credentials.password() == null) {
                return error(response, callback, 400, "username and password are required");
            }
            var session = authPort.login(credentials);
            var body = Map.of(
                "token", session.token(),
                "expires_at", session.expiresAt(),
                "user_id", session.userId()
            );
            byte[] bytes = json.toJsonBytes(body);
            response.setStatus(200);
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json");
            response.write(true, ByteBuffer.wrap(bytes), callback);
            return true;
        } catch (AppError.BadRequest e) {
            return error(response, callback, 400, e.getMessage());
        } catch (AppError.Unauthorized e) {
            return error(response, callback, 401, e.getMessage());
        } catch (Exception e) {
            LOG.warning("Login error: " + e.getMessage());
            return error(response, callback, 500, "Internal server error");
        }
    }

    private void applyCors(Response response) {
        response.getHeaders().put("Access-Control-Allow-Origin", config.corsOrigin());
        response.getHeaders().put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.getHeaders().put("Access-Control-Allow-Methods", "POST, OPTIONS");
    }

    private boolean error(Response response, Callback callback, int status, String message) {
        byte[] bytes = json.toJsonBytes(Map.of("error", message));
        response.setStatus(status);
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json");
        response.write(true, ByteBuffer.wrap(bytes), callback);
        return true;
    }
}
