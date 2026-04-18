+++
artifact_type = "task_file"
initiative    = "backend-bootstrap"
spec_id       = "SPEC-0001"
owner         = "rafex"
state         = "todo"
+++

# Tasks: backend-bootstrap

Tareas para crear la estructura Maven multi-modulo completa del backend.
Ejecutar en orden; cada tarea depende de la anterior.

---

+++
id             = "TASK-0001"
title          = "Crear pom.xml raiz (parent aggregator)"
state          = "todo"
dependencies   = []
expected_files = ["pom.xml", ".mvn/wrapper/maven-wrapper.properties", "mvnw", "mvnw.cmd"]
close_criteria = "./mvnw validate pasa sin errores"
validation     = ["./mvnw validate"]
+++

## TASK-0001: Crear pom.xml raiz

Crear el `pom.xml` raiz del proyecto con las siguientes caracteristicas:

```xml
<groupId>dev.rafex.chat</groupId>
<artifactId>ether-chat-backend</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>
```

**Parent de Ether:**
```xml
<parent>
    <groupId>dev.rafex.ether.parent</groupId>
    <artifactId>ether-parent</artifactId>
    <version>9.5.5</version>
</parent>
```

**Sobreescribir Java version** (ether-parent usa 25 por defecto):
```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <ether.version>9.5.5</ether.version>
    <sqlite.jdbc.version>3.49.1.0</sqlite.jdbc.version>
    <archunit.version>1.4.1</archunit.version>
    <slf4j.version>2.0.17</slf4j.version>
    <jetty.version>12.1.7</jetty.version>
</properties>
```

**Modulos a declarar:**
```xml
<modules>
    <module>ether-chat-backend-common</module>
    <module>ether-chat-backend-ports</module>
    <module>ether-chat-backend-core</module>
    <module>ether-chat-backend-infra-sqlite</module>
    <module>ether-chat-backend-bootstrap</module>
    <module>ether-chat-backend-transport-jetty</module>
    <module>ether-chat-backend-architecture-tests</module>
</modules>
```

**dependencyManagement** — declarar TODAS las dependencias de Ether con
sus groupIds exactos (ver STACK.md) mas SQLite, ArchUnit, SLF4J, Jetty,
Jackson, JUnit, Mockito, AssertJ.

Clave: las coordenadas exactas de Ether son:
- `dev.rafex.ether.config:ether-config:${ether.version}`
- `dev.rafex.ether.crypto:ether-crypto:${ether.version}`
- `dev.rafex.ether.jdbc:ether-jdbc:${ether.version}`
- `dev.rafex.ether.database:ether-database-core:${ether.version}`
- `dev.rafex.ether.json:ether-json:${ether.version}`
- `dev.rafex.ether.jwt:ether-jwt:${ether.version}`
- `dev.rafex.ether.observability:ether-observability-core:${ether.version}`
- `dev.rafex.ether.http:ether-http-problem:${ether.version}`
- `dev.rafex.ether.http:ether-http-jetty12:${ether.version}`
- `dev.rafex.ether.websocket:ether-websocket-jetty12:${ether.version}`
- `dev.rafex.ether.glowroot:ether-glowroot-jetty12:${ether.version}`
- `dev.rafex.ether.di:ether-di:1.0.0` (ether-di tiene version independiente)
- `org.xerial:sqlite-jdbc:${sqlite.jdbc.version}`
- `com.tngtech.archunit:archunit-junit5:${archunit.version}`
- `org.slf4j:slf4j-jdk14:${slf4j.version}`
- `org.eclipse.jetty:jetty-server:${jetty.version}`

**Nota:** `ether-di` puede tener version diferente a `ether.version`.
Verificar la version publicada en Maven Central para `dev.rafex.ether.di:ether-di`.

Instalar Maven Wrapper en el root:
```bash
mvn wrapper:wrapper -Dmaven=3.9.9
```
O crear manualmente `.mvn/wrapper/maven-wrapper.properties`:
```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
```

---

+++
id             = "TASK-0002"
title          = "Crear modulo ether-chat-backend-common"
state          = "todo"
dependencies   = ["TASK-0001"]
expected_files = ["ether-chat-backend-common/pom.xml",
                  "ether-chat-backend-common/src/main/java/dev/rafex/chat/shared/config/AppConfig.java",
                  "ether-chat-backend-common/src/main/java/dev/rafex/chat/shared/config/DatabaseConfig.java",
                  "ether-chat-backend-common/src/main/java/dev/rafex/chat/shared/config/ServerConfig.java",
                  "ether-chat-backend-common/src/main/java/dev/rafex/chat/shared/error/AppError.java",
                  "ether-chat-backend-common/src/main/java/dev/rafex/chat/shared/error/ChatErrorCode.java"]
close_criteria = "./mvnw compile -pl ether-chat-backend-common pasa"
validation     = ["./mvnw compile -pl ether-chat-backend-common"]
+++

## TASK-0002: Crear modulo common

**pom.xml del modulo:**
```xml
<parent>
    <groupId>dev.rafex.chat</groupId>
    <artifactId>ether-chat-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
<artifactId>ether-chat-backend-common</artifactId>
<packaging>jar</packaging>

<dependencies>
    <dependency>
        <groupId>dev.rafex.ether.config</groupId>
        <artifactId>ether-config</artifactId>
    </dependency>
</dependencies>
```

**AppConfig.java** — record que carga toda la config del sistema:
```java
package dev.rafex.chat.shared.config;

import dev.rafex.ether.config.EtherConfig;
import dev.rafex.ether.config.sources.EnvironmentConfigSource;
import dev.rafex.ether.config.sources.SystemPropertyConfigSource;
import java.util.Objects;

public record AppConfig(DatabaseConfig database, ServerConfig server) {
    public AppConfig {
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(server, "server");
    }
    private static volatile AppConfig INSTANCE;
    public static AppConfig load() {
        if (INSTANCE == null) {
            synchronized (AppConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = loadFrom(EtherConfig.of(
                        new EnvironmentConfigSource(),
                        new SystemPropertyConfigSource()
                    ));
                }
            }
        }
        return INSTANCE;
    }
    public static AppConfig loadFrom(EtherConfig config) {
        return new AppConfig(DatabaseConfig.from(config), ServerConfig.from(config));
    }
    public static void reset() { synchronized (AppConfig.class) { INSTANCE = null; } }
}
```

**DatabaseConfig.java:**
```java
package dev.rafex.chat.shared.config;
import dev.rafex.ether.config.EtherConfig;

public record DatabaseConfig(String authDbPath, String chatDbPath) {
    public static DatabaseConfig from(EtherConfig config) {
        return new DatabaseConfig(
            config.get("AUTH_DB_PATH").orElse("./data/auth.db"),
            config.get("CHAT_DB_PATH").orElse("./data/chat.db")
        );
    }
}
```

**ServerConfig.java:**
```java
package dev.rafex.chat.shared.config;
import dev.rafex.ether.config.EtherConfig;

public record ServerConfig(int port, boolean sandbox,
                           String jwtSecret, long jwtExpirySeconds) {
    public static ServerConfig from(EtherConfig config) {
        return new ServerConfig(
            config.get("SERVER_PORT").map(Integer::parseInt).orElse(8080),
            config.get("SERVER_SANDBOX").map(Boolean::parseBoolean).orElse(false),
            config.get("JWT_SECRET").orElseThrow(() ->
                new IllegalStateException("JWT_SECRET env var is required")),
            config.get("JWT_EXPIRY_SECONDS").map(Long::parseLong).orElse(86400L)
        );
    }
}
```

**AppError.java** — sealed class para errores del dominio:
```java
package dev.rafex.chat.shared.error;

public sealed class AppError extends RuntimeException
    permits AppError.Unauthorized, AppError.NotFound, AppError.BadRequest {

    public AppError(String message) { super(message); }

    public static final class Unauthorized extends AppError {
        public Unauthorized(String msg) { super(msg); }
    }
    public static final class NotFound extends AppError {
        public NotFound(String msg) { super(msg); }
    }
    public static final class BadRequest extends AppError {
        public BadRequest(String msg) { super(msg); }
    }
}
```

**ChatErrorCode.java:**
```java
package dev.rafex.chat.shared.error;

public enum ChatErrorCode {
    INVALID_CREDENTIALS,
    TOKEN_INVALID,
    TOKEN_EXPIRED,
    MESSAGE_REQUIRED,
    CONVERSATION_NOT_FOUND
}
```

Copiar `.mvn/wrapper/` + `mvnw` + `mvnw.cmd` del root a este modulo.

---

+++
id             = "TASK-0003"
title          = "Crear modulo ether-chat-backend-ports"
state          = "todo"
dependencies   = ["TASK-0002"]
expected_files = ["ether-chat-backend-ports/pom.xml",
                  "ether-chat-backend-ports/src/main/java/dev/rafex/chat/auth/port/UserRepository.java",
                  "ether-chat-backend-ports/src/main/java/dev/rafex/chat/auth/port/AuthPort.java",
                  "ether-chat-backend-ports/src/main/java/dev/rafex/chat/chat/port/ConversationRepository.java",
                  "ether-chat-backend-ports/src/main/java/dev/rafex/chat/chat/port/MessageRepository.java",
                  "ether-chat-backend-ports/src/main/java/dev/rafex/chat/chat/port/AiGateway.java"]
close_criteria = "./mvnw compile -pl ether-chat-backend-ports pasa"
validation     = ["./mvnw compile -pl ether-chat-backend-ports --also-make"]
+++

## TASK-0003: Crear modulo ports

**pom.xml:** depende solo de `ether-chat-backend-common`. Sin dependencias de Ether
(los puertos son Java puro).

**UserRepository.java:**
```java
package dev.rafex.chat.auth.port;
import dev.rafex.chat.auth.domain.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    User save(User user);
}
```

**AuthPort.java:**
```java
package dev.rafex.chat.auth.port;
import dev.rafex.chat.auth.domain.Credentials;
import dev.rafex.chat.auth.domain.Session;

public interface AuthPort {
    Session login(Credentials credentials);
}
```

**ConversationRepository.java:**
```java
package dev.rafex.chat.chat.port;
import dev.rafex.chat.chat.domain.Conversation;
import java.util.Optional;

public interface ConversationRepository {
    Optional<Conversation> findById(String id);
    Conversation save(Conversation conversation);
    Conversation findOrCreate(String userId, String conversationId);
}
```

**MessageRepository.java:**
```java
package dev.rafex.chat.chat.port;
import dev.rafex.chat.chat.domain.Message;
import java.util.List;

public interface MessageRepository {
    Message save(Message message);
    List<Message> findByConversationId(String conversationId);
}
```

**AiGateway.java:**
```java
package dev.rafex.chat.chat.port;
import dev.rafex.chat.chat.domain.Message;
import java.util.List;

public interface AiGateway {
    String generate(List<Message> history);
}
```

Copiar Maven Wrapper a este modulo.

---

+++
id             = "TASK-0004"
title          = "Crear modulo ether-chat-backend-core"
state          = "todo"
dependencies   = ["TASK-0003"]
expected_files = ["ether-chat-backend-core/pom.xml",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/auth/domain/User.java",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/auth/domain/Credentials.java",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/auth/domain/Session.java",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/auth/service/AuthService.java",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/auth/service/AuthServiceImpl.java",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/chat/domain/Conversation.java",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/chat/domain/Message.java",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/chat/domain/MessageRole.java",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/chat/service/ChatService.java",
                  "ether-chat-backend-core/src/main/java/dev/rafex/chat/chat/service/ChatServiceImpl.java"]
close_criteria = "./mvnw compile -pl ether-chat-backend-core --also-make pasa"
validation     = ["./mvnw compile -pl ether-chat-backend-core --also-make"]
+++

## TASK-0004: Crear modulo core

**pom.xml:** depende de `ports`, `common`, `ether-crypto`, `ether-jwt`.
NO puede depender de `infra-sqlite`, `bootstrap` ni `transport-jetty`.

**Records de dominio auth:**
```java
// User.java
public record User(Long id, String username, String passwordHash, String createdAt) {}

// Credentials.java
public record Credentials(String username, String password) {}

// Session.java
public record Session(String token, String expiresAt) {}
```

**Records de dominio chat:**
```java
// MessageRole.java
public enum MessageRole { user, agent }

// Message.java
public record Message(Long id, String conversationId, MessageRole role,
                      String content, String createdAt) {}

// Conversation.java
public record Conversation(String id, String userId, String createdAt, String updatedAt) {}
```

**AuthServiceImpl.java** — implementa `AuthPort`:
- Constructor: `AuthServiceImpl(UserRepository repo, ServerConfig config)`
- `login(Credentials)`:
  1. `repo.findByUsername(username)` → Optional<User>
  2. Si vacio: `throw new AppError.Unauthorized("Invalid credentials")`
  3. `EtherCrypto.verify(password, user.passwordHash())` → bool (consultar API de ether-crypto)
  4. Si false: `throw new AppError.Unauthorized("Invalid credentials")`
  5. Generar JWT con `ether-jwt`: claims `{"sub": username}`, expiry `config.jwtExpirySeconds()`
  6. Return `new Session(token, expiresAt.toString())`

**ChatServiceImpl.java** — implementa `ChatService`:
- Constructor: `ChatServiceImpl(ConversationRepository convRepo, MessageRepository msgRepo, AiGateway ai)`
- `sendMessage(String userId, String conversationId, String messageText)`:
  1. `convRepo.findOrCreate(userId, conversationId)` → Conversation
  2. `msgRepo.save(new Message(null, conv.id(), MessageRole.user, messageText, null))`
  3. `history = msgRepo.findByConversationId(conv.id())`
  4. `content = ai.generate(history)`
  5. `msgRepo.save(new Message(null, conv.id(), MessageRole.agent, content, null))`
  6. Return `new ChatResponse(content, conv.id())`

Copiar Maven Wrapper a este modulo.

---

+++
id             = "TASK-0005"
title          = "Crear modulo ether-chat-backend-infra-sqlite"
state          = "todo"
dependencies   = ["TASK-0004"]
expected_files = ["ether-chat-backend-infra-sqlite/pom.xml",
                  "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/auth/infra/AuthDb.java",
                  "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/auth/infra/UserRepositoryImpl.java",
                  "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/ChatDb.java",
                  "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/ConversationRepositoryImpl.java",
                  "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/MessageRepositoryImpl.java",
                  "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/EchoAiGateway.java"]
close_criteria = "./mvnw compile -pl ether-chat-backend-infra-sqlite --also-make pasa"
validation     = ["./mvnw compile -pl ether-chat-backend-infra-sqlite --also-make"]
+++

## TASK-0005: Crear modulo infra-sqlite

**pom.xml:** depende de `ports`, `common`, `ether-jdbc`, `ether-database-core`,
`ether-crypto`, `ether-json`, `sqlite-jdbc` (runtime).
NO puede depender de `core`, `bootstrap` ni `transport-jetty`.

**AuthDb.java** — inicializa auth.db:
```java
public final class AuthDb {
    private AuthDb() {}
    public static void init(DatabaseClient db) {
        db.execute("PRAGMA journal_mode=WAL;");
        db.execute("PRAGMA synchronous=NORMAL;");
        db.execute("PRAGMA cache_size=10000;");
        db.execute("PRAGMA temp_store=MEMORY;");
        db.execute("PRAGMA foreign_keys=ON;");
        db.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT    NOT NULL UNIQUE,
                password_hash TEXT    NOT NULL,
                created_at    TEXT    NOT NULL
                              DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
            )
        """);
    }
}
```

**UserRepositoryImpl.java** — implementa `UserRepository`:
- Constructor: `UserRepositoryImpl(DatabaseClient db)`
- `findByUsername(String)`: `SELECT * FROM users WHERE username=?` → `Optional<User>`
- `save(User)`: `INSERT INTO users(username,password_hash) VALUES(?,?)` retorna el
  usuario con el `id` generado.

**ChatDb.java** — inicializa chat.db con pragmas WAL y tablas
`conversations` y `messages` (schema en SPEC-0003).

**ConversationRepositoryImpl.java**, **MessageRepositoryImpl.java** — implementan
sus puertos con `DatabaseClient`. Las queries deben usar prepared statements.

**EchoAiGateway.java** — implementa `AiGateway` haciendo eco del ultimo mensaje
(ver codigo en SPEC-0003).

**SQLite connection helper:**
```java
// En AuthDb o como clase separada DbFactory
// URL: "jdbc:sqlite:" + path
// Crear directorio padre si no existe
// Pasar la Connection a DatabaseClient
```

Copiar Maven Wrapper a este modulo.

---

+++
id             = "TASK-0006"
title          = "Crear modulo ether-chat-backend-bootstrap"
state          = "todo"
dependencies   = ["TASK-0005"]
expected_files = ["ether-chat-backend-bootstrap/pom.xml",
                  "ether-chat-backend-bootstrap/src/main/java/dev/rafex/chat/bootstrap/AppContainer.java",
                  "ether-chat-backend-bootstrap/src/main/java/dev/rafex/chat/bootstrap/AppBootstrap.java"]
close_criteria = "./mvnw compile -pl ether-chat-backend-bootstrap --also-make pasa"
validation     = ["./mvnw compile -pl ether-chat-backend-bootstrap --also-make"]
+++

## TASK-0006: Crear modulo bootstrap

**pom.xml:** depende de `common`, `core`, `infra-sqlite`, `ether-di`,
`ether-config`, `ether-database-core`.
NO puede depender de `transport-jetty`.

**AppContainer.java** — record que contiene todas las dependencias cableadas:
```java
public record AppContainer(
    AuthPort authPort,
    ChatService chatService
) {}
```

**AppBootstrap.java** — construye el grafo de objetos explicitamente:
```java
public final class AppBootstrap implements AutoCloseable {
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

        // Auth DB
        var authConn = closer.register(openSqlite(config.database().authDbPath()));
        var authDb   = new DatabaseClient(authConn);   // ether-jdbc
        AuthDb.init(authDb);

        // Chat DB
        var chatConn = closer.register(openSqlite(config.database().chatDbPath()));
        var chatDb   = new DatabaseClient(chatConn);
        ChatDb.init(chatDb);

        // Repositories
        var userRepo  = new UserRepositoryImpl(authDb);
        var convRepo  = new ConversationRepositoryImpl(chatDb);
        var msgRepo   = new MessageRepositoryImpl(chatDb);

        // AiGateway (por defecto EchoAiGateway; sustituir aqui)
        AiGateway ai  = new EchoAiGateway();

        // Services
        var authService = new AuthServiceImpl(userRepo, config.server());
        var chatService = new ChatServiceImpl(convRepo, msgRepo, ai);

        // Seed demo user
        SeedUsers.seedIfAbsent(userRepo, config.server());

        return new AppBootstrap(closer, new AppContainer(authService, chatService));
    }

    private static Connection openSqlite(String path) {
        // Crear directorios padre, abrir JDBC connection
    }

    @Override public void close() throws Exception { closer.close(); }
}
```

Copiar Maven Wrapper a este modulo.

---

+++
id             = "TASK-0007"
title          = "Crear modulo ether-chat-backend-transport-jetty"
state          = "todo"
dependencies   = ["TASK-0006"]
expected_files = ["ether-chat-backend-transport-jetty/pom.xml",
                  "ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/App.java",
                  "ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/server/AppServer.java",
                  "ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/shared/handler/HealthHandler.java"]
close_criteria = "./mvnw package -pl ether-chat-backend-transport-jetty --also-make -DskipTests y el jar existe"
validation     = ["./mvnw package -pl ether-chat-backend-transport-jetty --also-make -DskipTests",
                  "ls ether-chat-backend-transport-jetty/target/*-jar-with-dependencies.jar"]
+++

## TASK-0007: Crear modulo transport-jetty

**pom.xml:** depende de `bootstrap`, `ether-http-jetty12`, `ether-websocket-jetty12`,
`ether-http-problem`, `ether-observability-core`, `ether-json`, `ether-jwt`,
`ether-glowroot-jetty12` (provided), `slf4j-jdk14` (runtime), `jetty-server`.
Configura `maven-assembly-plugin` para generar `jar-with-dependencies`.

**App.java:**
```java
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
        try (var runtime = AppBootstrap.start()) {
            AppServer.start(runtime.container());
        }
    }
}
```

**AppServer.java** usando `EtherServer.builder()`:
```java
var server = EtherServer.builder()
    .port(config.server().port())
    .handler("/health",            new HealthHandler())
    // Auth y Chat handlers se agregan en SPEC-0002 y SPEC-0003
    .build();
server.start();
server.join();
```

**HealthHandler.java:**
```java
// Responde GET /health con:
// {"status":"ok","version":"1.0.0-SNAPSHOT","timestamp":"<ISO-8601>"}
// Usar ether-json para serializar y ether-observability-core para timestamp
```

Copiar Maven Wrapper a este modulo.

---

+++
id             = "TASK-0008"
title          = "Crear modulo ether-chat-backend-architecture-tests"
state          = "todo"
dependencies   = ["TASK-0007"]
expected_files = ["ether-chat-backend-architecture-tests/pom.xml",
                  "ether-chat-backend-architecture-tests/src/test/java/dev/rafex/chat/arch/HexagonalArchitectureTest.java"]
close_criteria = "./mvnw test -pl ether-chat-backend-architecture-tests --also-make pasa"
validation     = ["./mvnw test -pl ether-chat-backend-architecture-tests --also-make"]
+++

## TASK-0008: Crear modulo architecture-tests

**pom.xml:** depende de TODOS los modulos anteriores y de `archunit-junit5`.
Solo contiene tests; no tiene codigo de produccion.

**HexagonalArchitectureTest.java:**
```java
package dev.rafex.chat.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "dev.rafex.chat")
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_layers = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Common").definedBy("dev.rafex.chat.shared..")
        .layer("Ports").definedBy("dev.rafex.chat..port..")
        .layer("Core").definedBy("dev.rafex.chat..domain..", "dev.rafex.chat..service..")
        .layer("Infra").definedBy("dev.rafex.chat..infra..")
        .layer("Bootstrap").definedBy("dev.rafex.chat.bootstrap..")
        .layer("Transport").definedBy("dev.rafex.chat.server..", "dev.rafex.chat..handler..", "dev.rafex.chat.App")
        // Ports no dependen de Core ni Infra
        .whereLayer("Ports").mayOnlyAccessLayers("Common")
        // Core solo ve Ports y Common
        .whereLayer("Core").mayOnlyAccessLayers("Ports", "Common")
        // Infra solo ve Ports y Common
        .whereLayer("Infra").mayOnlyAccessLayers("Ports", "Common")
        // Bootstrap cablea todo pero no accede a Transport
        .whereLayer("Bootstrap").mayOnlyAccessLayers("Core", "Infra", "Ports", "Common")
        // Transport ve Bootstrap y hacia arriba
        .whereLayer("Transport").mayOnlyAccessLayers("Bootstrap", "Core", "Ports", "Common");
}
```

Copiar Maven Wrapper a este modulo.

---

+++
id             = "TASK-0009"
title          = "Validacion final de SPEC-0001"
state          = "todo"
dependencies   = ["TASK-0008"]
expected_files = []
close_criteria = "./mvnw clean compile BUILD SUCCESS con 7 modulos"
validation     = ["./mvnw clean compile",
                  "JWT_SECRET=test-secret-min-32-characters ./mvnw package -DskipTests",
                  "curl http://localhost:8080/health retorna {status:ok}"]
+++

## TASK-0009: Validacion final

1. `./mvnw clean compile` — debe mostrar `BUILD SUCCESS` con los 7 modulos.
2. `./mvnw clean package -DskipTests` — debe generar el fat-jar.
3. Arrancar el jar con las variables minimas:
   ```bash
   JWT_SECRET=test-secret-minimum-32-characters! \
   java -jar ether-chat-backend-transport-jetty/target/*-jar-with-dependencies.jar
   ```
4. `curl http://localhost:8080/health` debe retornar:
   ```json
   {"status":"ok","version":"1.0.0-SNAPSHOT","timestamp":"<ISO>"}
   ```
5. Actualizar SPEC-0001 `state = "done"` en `agents/specs/backend-bootstrap/SPEC.md`.
6. Actualizar SPEC-0002 `state = "active"`.
7. Actualizar TRACEABILITY.md con el resultado.
