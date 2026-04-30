package dev.rafex.chat.server;

import dev.rafex.chat.auth.handler.LoginHandler;
import dev.rafex.chat.bootstrap.AppContainer;
import dev.rafex.chat.chat.handler.ChatMessageHandler;
import dev.rafex.chat.chat.handler.WebSocketChatHandler;
import dev.rafex.chat.shared.config.AppConfig;
import dev.rafex.chat.shared.handler.HealthHandler;
import dev.rafex.ether.http.jetty12.JettyServerConfig;
import dev.rafex.ether.http.jetty12.JettyServerFactory;
import dev.rafex.ether.http.jetty12.routing.JettyRouteRegistry;
import dev.rafex.ether.websocket.jetty12.JettyWebSocketRouteRegistry;
import dev.rafex.ether.websocket.jetty12.JettyWebSocketServerConfig;
import dev.rafex.ether.websocket.jetty12.JettyWebSocketServerFactory;
import java.util.logging.Logger;

public final class AppServer {
    private static final Logger LOG = Logger.getLogger(AppServer.class.getName());
    private AppServer() {}

    public static void start(AppContainer container) throws Exception {
        var json = container.json();
        var appConfig = AppConfig.load();
        var serverConfig = appConfig.server();

        var wsRegistry = new JettyWebSocketRouteRegistry();
        wsRegistry.add("/ws/chat", new WebSocketChatHandler(container.chatService(), json, serverConfig));
        var wsConfig = new JettyWebSocketServerConfig(serverConfig.wsPort(), 2, 10, 30000, "ws-pool");
        var wsRunner = JettyWebSocketServerFactory.create(wsConfig, wsRegistry);
        wsRunner.start();
        LOG.info("WebSocket server started on port " + serverConfig.wsPort());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { wsRunner.stop(); } catch (Exception e) { LOG.warning("WS shutdown error: " + e.getMessage()); }
        }));

        var srvCfg = serverConfig(appConfig);
        var registry = new JettyRouteRegistry();
        registry.add("/health", new HealthHandler(json));
        registry.add("/api/auth/login", new LoginHandler(container.authPort(), json));
        registry.add("/api/chat/message", new ChatMessageHandler(container.chatService(), json, serverConfig));
        var server = JettyServerFactory.create(srvCfg, registry, json);
        server.start();
        server.await();
        wsRunner.stop();
    }

    private static JettyServerConfig serverConfig(AppConfig appConfig) {
        var envConfig = JettyServerConfig.fromEnv();
        return new JettyServerConfig(
            envConfig.host(),
            appConfig.server().port(),
            envConfig.maxThreads(),
            envConfig.minThreads(),
            envConfig.idleTimeoutMs(),
            envConfig.threadPoolName(),
            envConfig.environment(),
            envConfig.acceptQueueSize(),
            envConfig.reuseAddress(),
            envConfig.stopAtShutdown(),
            envConfig.stopTimeoutMs(),
            envConfig.shutdownIdleTimeoutMs(),
            envConfig.trustForwardHeaders(),
            envConfig.forwardedOnly(),
            envConfig.inputBufferSize(),
            envConfig.outputBufferSize(),
            envConfig.requestHeaderSize(),
            envConfig.responseHeaderSize(),
            envConfig.minRequestDataRate(),
            envConfig.minResponseDataRate(),
            envConfig.maxErrorDispatches(),
            envConfig.maxUnconsumedRequestContentReads(),
            envConfig.maxRequestBodyBytes(),
            envConfig.maxResponseBodyBytes(),
            envConfig.maxConcurrentRequests(),
            envConfig.maxSuspendedRequests(),
            envConfig.maxSuspendMs(),
            envConfig.maxRequestsPerRemoteIp()
        );
    }
}
