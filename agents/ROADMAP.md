# ROADMAP.md

Direccion del proyecto en el tiempo.

## Base completada

- `SPEC-0001` backend-bootstrap: multi-modulo Maven operativo.
- `SPEC-0002` auth-service: `POST /api/auth/login` con seed `demo`.
- `SPEC-0003` chat-service: `POST /api/chat/message` con JWT y
  `EchoAiGateway`.

Validacion observada el 18 de abril de 2026:
- `mvn -DskipTests package`
- `GET /health`
- `POST /api/auth/login`
- `POST /api/chat/message`

## Siguiente

## Ahora — SPEC-0004: ai-realtime-mvp

Implementar proveedor real con `ether-ai-deepseek` y soporte realtime
MVP en `WS /ws/chat` usando `ether-websocket-jetty12`.

**Entregables:**

- REST `POST /api/chat/message` usa DeepSeek cuando `AI_PROVIDER=deepseek`
- fallback a `EchoAiGateway` cuando no hay config
- `WS /ws/chat` con JWT valido responde `{content, conversation_id}`

Tareas en: `tasks/ai-realtime-mvp/TASKS.md`

## Despues

- Streaming SSE desde `ChatMessageHandler` (token-by-token)
- Tests de integracion con Testcontainers (cuando se necesite)
- Docker image con jlink/jpackage o fat-jar

## No hacer por ahora

- Multitenancy
- Registro y administracion de usuarios via API
- Migraciones versionadas (Flyway/Liquibase) — el schema se aplica al
  arrancar con `IF NOT EXISTS`
- Cambio de motor a Postgres — crear `infra-postgres` si se necesita,
  sin tocar `core` ni `ports`
