package dev.rafex.chat.chat.infra;

import dev.rafex.chat.chat.domain.Message;
import dev.rafex.chat.chat.domain.MessageRole;
import dev.rafex.ether.json.JsonCodecBuilder;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeepseekAiGatewayTest {

    @Test
    void returnsContentFromSuccessfulResponse() throws Exception {
        var responseBody = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello there!\"}}]}"
            .getBytes(StandardCharsets.UTF_8);
        var gateway = gatewayWith(200, responseBody);

        var history = List.of(new Message(null, null, MessageRole.user, "Hello", null));
        var result = gateway.generate(history);

        assertEquals("Hello there!", result);
    }

    @Test
    void throwsOnProviderHttpError() {
        var gateway = gatewayWith(401, new byte[0]);

        var history = List.of(new Message(null, null, MessageRole.user, "Hello", null));
        assertThrows(IllegalStateException.class, () -> gateway.generate(history));
    }

    @Test
    void throwsOnNetworkFailure() {
        var json = JsonCodecBuilder.lenient().build();
        var client = failingClient(new IOException("Network error"));
        var gateway = new DeepseekAiGateway(client, json, "key", "deepseek-chat", "https://api.deepseek.com/v1");

        var history = List.of(new Message(null, null, MessageRole.user, "Hello", null));
        assertThrows(IllegalStateException.class, () -> gateway.generate(history));
    }

    @Test
    void handlesEmptyHistory() throws Exception {
        var responseBody = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"No input\"}}]}"
            .getBytes(StandardCharsets.UTF_8);
        var gateway = gatewayWith(200, responseBody);

        var result = gateway.generate(List.of());

        assertEquals("No input", result);
    }

    private static DeepseekAiGateway gatewayWith(int statusCode, byte[] body) {
        var json = JsonCodecBuilder.lenient().build();
        return new DeepseekAiGateway(fixedResponseClient(statusCode, body), json,
            "test-key", "deepseek-chat", "https://api.deepseek.com/v1");
    }

    private static HttpClient fixedResponseClient(int statusCode, byte[] body) {
        return new StubHttpClient() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
                return new StubHttpResponse<>((T) body, statusCode);
            }
        };
    }

    private static HttpClient failingClient(IOException error) {
        return new StubHttpClient() {
            @Override
            public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
                throw error;
            }
        };
    }

    private abstract static class StubHttpClient extends HttpClient {
        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NORMAL; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return null; }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            throw new UnsupportedOperationException();
        }
        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler, HttpResponse.PushPromiseHandler<T> push) {
            throw new UnsupportedOperationException();
        }
    }

    private record StubHttpResponse<T>(T body, int statusCode) implements HttpResponse<T> {
        @Override public java.net.URI uri() { return null; }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        @Override public HttpHeaders headers() { return HttpHeaders.of(java.util.Map.of(), (a, b) -> true); }
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public HttpRequest request() { return null; }
        @Override public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }
    }
}
