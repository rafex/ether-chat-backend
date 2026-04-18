+++
artifact_type = "spec"
id            = "SPEC-0003"
state         = "draft"
owner         = "rafex"
created_at    = "2026-04-17"
updated_at    = "2026-04-17"
replaces      = "none"
related_tasks       = ["TASK-0020", "TASK-0021", "TASK-0022", "TASK-0023",
                       "TASK-0024", "TASK-0025", "TASK-0026"]
related_decisions   = ["DEC-0001", "DEC-0005"]
artifacts           = ["ether-chat-backend-ports/src/main/java/dev/rafex/chat/chat/port/",
                       "ether-chat-backend-core/src/main/java/dev/rafex/chat/chat/",
                       "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/",
                       "ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/chat/handler/"]
validation          = ["./mvnw test -pl ether-chat-backend-core",
                       "curl POST /api/chat/message con token retorna 200"]
+++

# SPEC-0003: chat-service

## Resumen

Implementar el endpoint `POST /api/chat/message` con persistencia de
conversaciones y mensajes en `chat.db` (SQLite WAL), verificacion de
JWT por request, y el puerto `AiGateway` con la implementacion
`EchoAiGateway` por defecto.

## Problema

El frontend envia mensajes con un JWT y espera recibir la respuesta del
agente junto con el `conversation_id` para continuar la conversacion.
Sin este endpoint el chat no funciona en absoluto.

## Objetivo

`POST /api/chat/message` funciona end-to-end: verifica el JWT, persiste
el mensaje del usuario, llama a `AiGateway`, persiste la respuesta y
retorna `{content, conversation_id}`.

## Alcance

**Incluye:**
- Schema SQL para tablas `conversations` y `messages` en `chat.db`
- `ChatDb.init()`: aplica schema y pragmas WAL
- Records de dominio: `Conversation`, `Message`, `MessageRole` (enum user/agent)
- Interfaces de puerto: `ConversationRepository`, `MessageRepository`, `AiGateway`
- `ChatServiceImpl`: logica de negocio (get/create conversation, save messages,
  call AiGateway)
- `ConversationRepositoryImpl`, `MessageRepositoryImpl` con `DatabaseClient`
- `EchoAiGateway`: implementacion por defecto que hace eco del ultimo mensaje
- `ChatMessageHandler`: maneja `POST /api/chat/message`, extrae y verifica JWT,
  delega a `ChatService`
- Tests unitarios de `ChatServiceImpl` con mocks
- Tests de repositorios con SQLite en memoria

**Excluye:**
- Streaming SSE (futuro)
- WebSocket (futuro)
- Integracion con proveedor de IA real
- Historial de conversacion paginado via API

## Requisitos funcionales

- RF-1: `POST /api/chat/message` con JWT valido y body
  `{"message":"Hola","conversation_id":null}` retorna `200` con
  `{"content":"<string>","conversation_id":"<string>"}`.
- RF-2: En la segunda peticion con el mismo `conversation_id`, la
  conversacion ya existente se reutiliza.
- RF-3: `POST /api/chat/message` sin `Authorization` header retorna
  `401` con body RFC 7807.
- RF-4: `POST /api/chat/message` con JWT invalido o expirado retorna
  `401` con body RFC 7807.
- RF-5: `POST /api/chat/message` sin campo `message` retorna `400`.
- RF-6: El `conversation_id` en la respuesta es un UUID v4 generado
  al crear la conversacion.
- RF-7: Cada mensaje del usuario y cada respuesta del agente se
  persisten en la tabla `messages`.
- RF-8: `ChatDb.init()` crea las tablas con `CREATE TABLE IF NOT EXISTS`
  y aplica los pragmas WAL.

## Schema SQL

```sql
CREATE TABLE IF NOT EXISTS conversations (
    id         TEXT    PRIMARY KEY,
    user_id    TEXT    NOT NULL,
    created_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now')),
    updated_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
);

CREATE TABLE IF NOT EXISTS messages (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    conversation_id TEXT    NOT NULL REFERENCES conversations(id),
    role            TEXT    NOT NULL CHECK (role IN ('user','agent')),
    content         TEXT    NOT NULL,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation
    ON messages(conversation_id, created_at);
```

## Pragmas WAL (chat.db)

```sql
PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA cache_size=10000;
PRAGMA temp_store=MEMORY;
PRAGMA foreign_keys=ON;
```

## Requisitos no funcionales

- RNF-1: La verificacion del JWT usa `ether-jwt` con el mismo `JWT_SECRET`.
- RNF-2: `ChatMessageHandler` extrae el `sub` del JWT como `userId`.
- RNF-3: `EchoAiGateway` repite el ultimo mensaje del usuario con el
  prefijo `"[ECHO] "`. Es el comportamiento por defecto en ausencia de
  integracion real.
- RNF-4: `AiGateway.generate()` recibe la lista de mensajes de la
  conversacion (historial completo) para permitir contexto multi-turn.

## Criterios de aceptacion

- Dado el backend arrancado con token valido para `demo`
- Cuando `POST /api/chat/message {"message":"Hola","conversation_id":null}`
- Entonces responde `200` con `{"content":"[ECHO] Hola","conversation_id":"<uuid>"}`

- Dado el mismo `conversation_id` retornado
- Cuando se hace otra peticion con ese `conversation_id`
- Entonces la respuesta incluye el mismo `conversation_id`

- Dado el backend arrancado
- Cuando `POST /api/chat/message` sin `Authorization`
- Entonces responde `401`

## Dependencias

- SPEC-0001 `done`: el proyecto Maven compila.
- SPEC-0002 `done`: `POST /api/auth/login` funciona y provee JWT.

## Puerto AiGateway

```java
package dev.rafex.chat.chat.port;

import dev.rafex.chat.chat.domain.Message;
import java.util.List;

public interface AiGateway {
    /**
     * Genera una respuesta dado el historial de mensajes de la conversacion.
     * @param history todos los mensajes ordenados por created_at ASC
     * @return contenido de la respuesta del agente
     */
    String generate(List<Message> history);
}
```

## EchoAiGateway (implementacion por defecto)

```java
package dev.rafex.chat.chat.infra;

import dev.rafex.chat.chat.domain.Message;
import dev.rafex.chat.chat.port.AiGateway;
import java.util.List;

public final class EchoAiGateway implements AiGateway {
    @Override
    public String generate(List<Message> history) {
        if (history.isEmpty()) return "[ECHO] (empty)";
        var last = history.get(history.size() - 1);
        return "[ECHO] " + last.content();
    }
}
```

## ChatMessageHandler (esqueleto)

```java
// POST /api/chat/message
// 1. Leer Authorization: Bearer <token>
// 2. ether-jwt: verify(token) → claims; extraer sub como userId
// 3. Leer body JSON → {message, conversation_id?}
// 4. Validar que message no sea null/blank → 400
// 5. chatService.sendMessage(userId, conversationId, message)
// 6. Retornar {"content": "...", "conversation_id": "..."}
// En JwtException → 401 RFC 7807
// En IllegalArgumentException → 400 RFC 7807
```
