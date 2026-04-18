package dev.rafex.chat.auth.handler;

import dev.rafex.chat.auth.domain.Credentials;
import dev.rafex.chat.auth.port.AuthPort;
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

    public LoginHandler(AuthPort authPort, JsonCodec json) {
        this.authPort = Objects.requireNonNull(authPort, "authPort");
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(405); callback.succeeded(); return true;
        }
        try {
            InputStream is = Request.asInputStream(request);
            var credentials = json.readValue(is, Credentials.class);
            if (credentials == null || credentials.username() == null || credentials.password() == null) {
                return error(response, callback, 400, "username and password are required");
            }
            var session = authPort.login(credentials);
            byte[] bytes = json.toJsonBytes(Map.of("token", session.token()));
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

    private boolean error(Response response, Callback callback, int status, String detail) {
        byte[] bytes = json.toJsonBytes(Map.of("status", status, "title", status == 400 ? "Bad Request" : status == 401 ? "Unauthorized" : "Error", "detail", detail));
        response.setStatus(status);
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/problem+json");
        response.write(true, ByteBuffer.wrap(bytes), callback);
        return true;
    }
}
