package dev.rafex.chat.bootstrap;

import dev.rafex.chat.auth.infra.AuthDb;
import dev.rafex.chat.auth.infra.UserRepositoryImpl;
import dev.rafex.chat.auth.port.AuthPort;
import dev.rafex.chat.auth.service.AuthServiceImpl;
import dev.rafex.chat.chat.infra.ChatDb;
import dev.rafex.chat.chat.infra.ConversationRepositoryImpl;
import dev.rafex.chat.chat.infra.DeepseekAiGateway;
import dev.rafex.chat.chat.infra.EchoAiGateway;
import dev.rafex.chat.chat.infra.MessageRepositoryImpl;
import dev.rafex.chat.chat.port.AiGateway;
import dev.rafex.chat.chat.port.ChatService;
import dev.rafex.chat.chat.service.ChatServiceImpl;
import dev.rafex.chat.shared.config.AppConfig;
import dev.rafex.ether.di.Closer;
import dev.rafex.ether.jdbc.client.JdbcDatabaseClient;
import dev.rafex.ether.jdbc.datasource.SimpleDataSource;
import dev.rafex.ether.json.JsonCodecBuilder;
import java.io.File;
import java.util.logging.Logger;

public final class AppBootstrap implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(AppBootstrap.class.getName());
    private final Closer closer;
    private final AppContainer container;

    private AppBootstrap(Closer closer, AppContainer container) {
        this.closer = closer;
        this.container = container;
    }

    public AppContainer container() { return container; }

    public static AppBootstrap start() {
        var config = AppConfig.load();
        var closer = new Closer();

        var authPath = config.database().authDbPath();
        new File(authPath).getParentFile().mkdirs();
        var authDs = new SimpleDataSource("jdbc:sqlite:" + authPath);
        var authDb = new JdbcDatabaseClient(authDs);
        AuthDb.init(authDb);

        var chatPath = config.database().chatDbPath();
        new File(chatPath).getParentFile().mkdirs();
        var chatDs = new SimpleDataSource("jdbc:sqlite:" + chatPath);
        var chatDb = new JdbcDatabaseClient(chatDs);
        ChatDb.init(chatDb);

        var userRepo = new UserRepositoryImpl(authDb);
        var convRepo = new ConversationRepositoryImpl(chatDb);
        var msgRepo  = new MessageRepositoryImpl(chatDb);
        var json = JsonCodecBuilder.lenient().build();

        SeedUsers.seedIfAbsent(userRepo);

        AuthPort authPort = new AuthServiceImpl(userRepo, config.server());
        ChatService chatService = new ChatServiceImpl(convRepo, msgRepo, aiGateway(config, json));

        return new AppBootstrap(closer, new AppContainer(authPort, chatService, json));
    }

    private static AiGateway aiGateway(AppConfig config, dev.rafex.ether.json.JsonCodec json) {
        var ai = config.ai();
        if (!"deepseek".equals(ai.provider())) {
            return new EchoAiGateway();
        }
        if (ai.deepseekApiKey().isEmpty()) {
            LOG.warning("AI_PROVIDER=deepseek but DEEPSEEK_API_KEY is missing. Falling back to EchoAiGateway.");
            return new EchoAiGateway();
        }
        return new DeepseekAiGateway(json, ai.deepseekApiKey().get(), ai.deepseekModel(), ai.deepseekBaseUrl());
    }

    @Override
    public void close() { closer.close(); }
}
