package dev.rafex.chat;

import dev.rafex.chat.bootstrap.AppBootstrap;
import dev.rafex.chat.server.AppServer;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

public final class App {
    private static final Logger LOG = Logger.getLogger(App.class.getName());
    private App() {}

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        LOG.info("Starting ether-chat-backend...");
        try (var bootstrap = AppBootstrap.start()) {
            AppServer.start(bootstrap.container());
        }
    }
}
