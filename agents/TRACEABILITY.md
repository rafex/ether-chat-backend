# TRACEABILITY.md

Mapa de relaciones entre specs, tareas, decisiones y artefactos.
Actualizar al cerrar cada iniciativa.

## Tabla de trazabilidad

| Spec | Estado | Tareas | Decisiones | Archivos principales | Validacion |
|------|--------|--------|-----------|---------------------|-----------|
| SPEC-0001 | `done` | tasks/backend-bootstrap/TASKS.md | DEC-0002, DEC-0003, DEC-0004 | pom.xml raiz y todos los modulos | `mvn -DskipTests package`, `GET /health` |
| SPEC-0002 | `done` | tasks/auth-service/TASKS.md | DEC-0001, DEC-0006 | `*-auth-*`, `AuthServiceImpl`, `LoginHandler` | `POST /api/auth/login` retorna 200 con token |
| SPEC-0003 | `done` | tasks/chat-service/TASKS.md | DEC-0001, DEC-0005 | `*-chat-*`, `ConversationRepositoryImpl`, `ChatMessageHandler`, `openapi/openapi.yaml` | `POST /api/chat/message` retorna 200 con JWT valido |
| SPEC-0004 | `active` | tasks/ai-realtime-mvp/TASKS.md | DEC-0005, DEC-0006 | `DeepseekAiGateway`, `WebSocketChatHandler`, `AppBootstrap`, `AppServer` | `POST /api/chat/message` con deepseek y `WS /ws/chat` con JWT |

## Decisiones y sus consecuencias en artefactos

| Decision | Artefactos afectados |
|----------|---------------------|
| DEC-0001 SQLite WAL | `*-infra-sqlite/`: `AuthDb.java`, `ChatDb.java` |
| DEC-0002 Hexagonal | Todos los modulos Maven; estructura de packages |
| DEC-0003 ether-di | `*-bootstrap/`: `AppBootstrap.java`, `AppContainer.java` |
| DEC-0004 Maven Wrapper | `.mvn/wrapper/` en todos los modulos |
| DEC-0005 AiGateway | `*-ports/`: `AiGateway.java`; `*-infra-sqlite/`: `EchoAiGateway.java` |
| DEC-0006 JWT HS256 | `*-core/`: `AuthServiceImpl.java`; `*-transport-jetty/`: `ChatMessageHandler.java` |

## Evidencia reciente

- 2026-04-18: `mvn -pl ether-chat-backend-infra-sqlite -am -Dtest=UserRepositoryImplTest,ConversationRepositoryImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 2026-04-18: `mvn -pl ether-chat-backend-transport-jetty -am -Dtest=ChatMessageHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 2026-04-18: `mvn -DskipTests package`
- 2026-04-18: smoke manual con `GET /health`, `POST /api/auth/login` y `POST /api/chat/message`
