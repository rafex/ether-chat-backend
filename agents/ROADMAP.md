# ROADMAP.md

Direccion del proyecto en el tiempo.

## Ahora — SPEC-0001: backend-bootstrap

Crear la estructura Maven multi-modulo completa siguiendo el patron de
`ether-archetype` con SQLite en lugar de Postgres.

**Entregable:** `./mvnw clean compile` pasa desde la raiz con los 7 modulos.

Tareas en: `tasks/backend-bootstrap/TASKS.md`

## Despues — SPEC-0002: auth-service

Implementar `POST /api/auth/login` con:
- Tabla `users` en `auth.db` (SQLite WAL)
- Hash de contrasena con `ether-crypto`
- Emision de JWT con `ether-jwt`
- `LoginHandler` en `transport-jetty`

**Entregable:** `POST /api/auth/login` retorna `{token}` y los tests pasan.

Tareas en: `tasks/auth-service/TASKS.md`

## Despues — SPEC-0003: chat-service

Implementar `POST /api/chat/message` con:
- Tablas `conversations` y `messages` en `chat.db` (SQLite WAL)
- Verificacion JWT en cada request
- Puerto `AiGateway` con implementacion `EchoAiGateway`
- `ChatMessageHandler` en `transport-jetty`

**Entregable:** `POST /api/chat/message` con token valido retorna `{content, conversation_id}`.

Tareas en: `tasks/chat-service/TASKS.md`

## Mas adelante

- `AiGateway` para OpenAI (`POST /v1/chat/completions`)
- `AiGateway` para Ollama (compatible OpenAI)
- Streaming SSE desde `ChatMessageHandler`
- `WebSocketChatHandler` para streaming bidireccional
- Tests de integracion con Testcontainers (cuando se necesite)
- Docker image con jlink/jpackage o fat-jar

## No hacer por ahora

- Multitenancy
- Registro y administracion de usuarios via API
- Migraciones versionadas (Flyway/Liquibase) — el schema se aplica al
  arrancar con `IF NOT EXISTS`
- Cambio de motor a Postgres — crear `infra-postgres` si se necesita,
  sin tocar `core` ni `ports`
