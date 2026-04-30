package dev.rafex.chat.shared.handler;

import dev.rafex.ether.json.JsonCodec;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class HealthHandler extends Handler.Abstract.NonBlocking {
    private static final String VERSION = "1.0.0-SNAPSHOT";
    private final JsonCodec json;

    public HealthHandler(JsonCodec json) {
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        response.getHeaders().put("Access-Control-Allow-Origin", "*");
        response.getHeaders().put("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(204);
            callback.succeeded();
            return true;
        }
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(405);
            callback.succeeded();
            return true;
        }
        var body = Map.of("status", "ok", "version", VERSION, "timestamp", Instant.now().toString());
        byte[] bytes = json.toJsonBytes(body);
        response.setStatus(200);
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json");
        response.write(true, ByteBuffer.wrap(bytes), callback);
        return true;
    }
}
