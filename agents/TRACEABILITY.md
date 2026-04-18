# TRACEABILITY.md

Mapa de relaciones entre specs, tareas, decisiones y artefactos.
Actualizar al cerrar cada iniciativa.

## Tabla de trazabilidad

| Spec | Estado | Tareas | Decisiones | Archivos principales | Validacion |
|------|--------|--------|-----------|---------------------|-----------|
| SPEC-0001 | `active` | tasks/backend-bootstrap/TASKS.md | DEC-0002, DEC-0003, DEC-0004 | pom.xml raiz y todos los modulos | `./mvnw clean compile` |
| SPEC-0002 | `draft` | tasks/auth-service/TASKS.md | DEC-0001, DEC-0005, DEC-0006 | `*-ports/`, `*-core/`, `*-infra-sqlite/`, `*-transport-jetty/` | `POST /api/auth/login` retorna 200 |
| SPEC-0003 | `draft` | tasks/chat-service/TASKS.md | DEC-0001, DEC-0005 | `*-ports/`, `*-core/`, `*-infra-sqlite/`, `*-transport-jetty/` | `POST /api/chat/message` retorna 200 |

## Decisiones y sus consecuencias en artefactos

| Decision | Artefactos afectados |
|----------|---------------------|
| DEC-0001 SQLite WAL | `*-infra-sqlite/`: `AuthDb.java`, `ChatDb.java` |
| DEC-0002 Hexagonal | Todos los modulos Maven; estructura de packages |
| DEC-0003 ether-di | `*-bootstrap/`: `AppBootstrap.java`, `AppContainer.java` |
| DEC-0004 Maven Wrapper | `.mvn/wrapper/` en todos los modulos |
| DEC-0005 AiGateway | `*-ports/`: `AiGateway.java`; `*-infra-sqlite/`: `EchoAiGateway.java` |
| DEC-0006 JWT HS256 | `*-core/`: `AuthServiceImpl.java`; `*-transport-jetty/`: `ChatMessageHandler.java` |
