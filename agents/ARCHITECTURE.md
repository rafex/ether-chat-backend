# ARCHITECTURE.md

Describe la arquitectura actual del proyecto.

## Vision general

El backend es un proceso JVM unico con arquitectura hexagonal organizada
en modulos Maven. Expone HTTP REST y WebSocket via Jetty 12 embebido.
El dominio esta dividido en dos bounded contexts: `auth` (autenticacion
y JWT) y `chat` (conversaciones y mensajes). Cada context usa su propio
archivo SQLite con WAL. El adaptador de IA (`AiGateway`) es un puerto
que desacopla la logica de negocio del proveedor concreto de lenguaje.

## Estructura de modulos Maven

```
ether-chat-backend/                         <- parent pom (aggregator)
  ether-chat-backend-common/                <- config records, errores compartidos
  ether-chat-backend-ports/                 <- interfaces hexagonales (puertos)
  ether-chat-backend-core/                  <- logica de dominio (use cases)
  ether-chat-backend-infra-sqlite/          <- adaptadores SQLite (implementaciones)
  ether-chat-backend-bootstrap/             <- cableado DI (AppContainer, AppBootstrap)
  ether-chat-backend-transport-jetty/       <- entrada HTTP/WS (App, handlers)
  ether-chat-backend-architecture-tests/    <- tests ArchUnit de capas
```

Cada modulo tiene su propio Maven Wrapper (`.mvn/wrapper/` + `mvnw`).

## Grafo de dependencias Maven

```
transport-jetty  ──►  bootstrap  ──►  core        ──►  ports  ──►  common
                                  ──►  infra-sqlite ──►  ports  ──►  common
architecture-tests ──►  (todos los modulos anteriores)
```

**Dependencias prohibidas:**
- `core` NO puede depender de `infra-sqlite`, `bootstrap`, ni `transport-jetty`.
- `ports` NO puede depender de ningun otro modulo interno.
- `infra-sqlite` NO puede depender de `core`, `bootstrap`, ni `transport-jetty`.
- `bootstrap` NO puede depender de `transport-jetty`.

## Bounded contexts y packages

### Bounded context: auth

```
dev.rafex.chat.auth.domain          <- User, Credentials, Session (records Java)
dev.rafex.chat.auth.port            <- UserRepository (interface), AuthPort (interface)
dev.rafex.chat.auth.service         <- AuthService (interface), AuthServiceImpl
dev.rafex.chat.auth.infra           <- UserRepositoryImpl (SQLite), AuthDb (WAL init)
dev.rafex.chat.auth.handler         <- LoginHandler (POST /api/auth/login)
```

### Bounded context: chat

```
dev.rafex.chat.chat.domain          <- Conversation, Message, ConversationId (records)
dev.rafex.chat.chat.port            <- ConversationRepository, MessageRepository,
                                       AiGateway (interface)
dev.rafex.chat.chat.service         <- ChatService (interface), ChatServiceImpl
dev.rafex.chat.chat.infra           <- ConversationRepositoryImpl, MessageRepositoryImpl,
                                       ChatDb (WAL init), EchoAiGateway (implementacion por defecto)
dev.rafex.chat.chat.handler         <- ChatMessageHandler (POST /api/chat/message),
                                       WebSocketChatHandler (WS /ws/chat)
```

### Compartido

```
dev.rafex.chat.shared.config        <- AppConfig, DatabaseConfig, ServerConfig (records)
dev.rafex.chat.shared.error         <- AppError (sealed), ChatErrorCode (enum)
dev.rafex.chat.bootstrap            <- AppContainer, AppBootstrap (Closer)
dev.rafex.chat.server               <- AppServer (EtherServer builder)
dev.rafex.chat                      <- App (main class)
```

## Flujo principal: autenticacion

```
POST /api/auth/login
  LoginHandler.handle()
    └─ AuthService.login(Credentials)
         └─ UserRepository.findByUsername(username)
         └─ ether-crypto: verifyPassword(raw, hash)
         └─ ether-jwt: issue(claims, expiry)
    └─ responde {token}
```

## Flujo principal: mensaje de chat

```
POST /api/chat/message  [Authorization: Bearer <token>]
  ChatMessageHandler.handle()
    └─ ether-jwt: verify(token)   [extrae userId]
    └─ ChatService.sendMessage(userId, conversationId?, message)
         └─ ConversationRepository.findOrCreate(userId, conversationId)
         └─ MessageRepository.save(Message{role=user, content})
         └─ AiGateway.generate(conversation history)   [puerto]
         └─ MessageRepository.save(Message{role=agent, content})
    └─ responde {content, conversation_id}
```

## Inicializacion de SQLite WAL

Cada modulo de base de datos ejecuta al arrancar:

```sql
PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA cache_size=10000;
PRAGMA temp_store=MEMORY;
PRAGMA foreign_keys=ON;
```

## Variables de entorno

| Variable | Default | Descripcion |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `SERVER_SANDBOX` | `false` | `true` activa logging DEBUG |
| `AUTH_DB_PATH` | `./data/auth.db` | Path del archivo SQLite de auth |
| `CHAT_DB_PATH` | `./data/chat.db` | Path del archivo SQLite de chat |
| `JWT_SECRET` | — | Secreto HMAC HS256 (minimo 32 chars, requerido) |
| `JWT_EXPIRY_SECONDS` | `86400` | Duracion del token en segundos |
| `AI_PROVIDER` | `echo` | Selector de proveedor AI: `echo` o `deepseek` |
| `DEEPSEEK_API_KEY` | — | API key para DeepSeek cuando `AI_PROVIDER=deepseek` |
| `DEEPSEEK_MODEL` | `deepseek-chat` | Modelo a usar en DeepSeek |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com/v1` | URL base del API de DeepSeek |

## API Contract

Fuente de verdad para REST: `openapi/openapi.yaml`.

```
POST /api/auth/login
  Body:     { "username": "string", "password": "string" }
  200:      { "token": "string" }
  400:      RFC 7807 — campos faltantes
  401:      RFC 7807 — credenciales invalidas

POST /api/chat/message
  Headers:  Authorization: Bearer <token>
  Body:     { "message": "string", "conversation_id": "string|null" }
  200:      { "content": "string", "conversation_id": "string" }
  400:      RFC 7807 — mensaje faltante
  401:      RFC 7807 — token invalido o ausente

GET /health
  200:      { "status": "ok", "version": "string", "timestamp": "ISO-8601" }

WS /ws/chat          [futuro — realtime MVP de SPEC-0004]
  Headers:  Authorization: Bearer <token>
```

## Restricciones

- Handlers NO deben contener logica de negocio; solo traducen HTTP ↔ dominio.
- Los puertos (interfaces) definen el contrato; las implementaciones viven en infra.
- `App.java` solo configura logging y delega a `AppBootstrap` y `AppServer`.
- El schema SQL se aplica al arrancar en `AuthDb.init()` y `ChatDb.init()`.

## Riesgos

- SQLite en modo WAL soporta multiples lectores concurrentes pero un solo
  escritor. Para cargas muy altas de escritura considerar migrar a Postgres
  usando un nuevo modulo `infra-postgres` sin tocar `core` ni `ports`.
- `EchoAiGateway` (implementacion por defecto) solo hace eco; un integrador
  debe proveer su propia implementacion de `AiGateway` y cablearla en
  `AppBootstrap`.
