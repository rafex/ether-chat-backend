+++
artifact_type = "task_file"
initiative    = "chat-service"
spec_id       = "SPEC-0003"
owner         = "rafex"
state         = "todo"
+++

# Tasks: chat-service

Tareas para implementar `POST /api/chat/message`. Requiere SPEC-0002 done.

---

+++
id             = "TASK-0020"
title          = "Implementar ChatDb.init() con schema SQL y pragmas WAL"
state          = "todo"
dependencies   = []
expected_files = ["ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/ChatDb.java",
                  "ether-chat-backend-infra-sqlite/src/test/java/dev/rafex/chat/chat/infra/ChatDbTest.java"]
close_criteria = "ChatDb.init() no lanza excepciones con SQLite en memoria"
validation     = ["./mvnw test -pl ether-chat-backend-infra-sqlite -Dtest=ChatDbTest"]
+++

## TASK-0020: Implementar ChatDb

Crear `ChatDb.init(DatabaseClient db)` con:
1. Los 5 pragmas WAL identicos a `AuthDb`.
2. `CREATE TABLE IF NOT EXISTS conversations (...)` — schema en SPEC-0003.
3. `CREATE TABLE IF NOT EXISTS messages (...)` — schema en SPEC-0003.
4. `CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(...)`.

Test `ChatDbTest`:
- Verificar que las dos tablas existen despues de `init()`.
- Verificar que el index existe.

---

+++
id             = "TASK-0021"
title          = "Implementar ConversationRepositoryImpl y MessageRepositoryImpl"
state          = "todo"
dependencies   = ["TASK-0020"]
expected_files = ["ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/ConversationRepositoryImpl.java",
                  "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/MessageRepositoryImpl.java",
                  "ether-chat-backend-infra-sqlite/src/test/java/dev/rafex/chat/chat/infra/ConversationRepositoryImplTest.java",
                  "ether-chat-backend-infra-sqlite/src/test/java/dev/rafex/chat/chat/infra/MessageRepositoryImplTest.java"]
close_criteria = "Tests de ambos repositorios pasan con SQLite en memoria"
validation     = ["./mvnw test -pl ether-chat-backend-infra-sqlite -Dtest=ConversationRepositoryImplTest,MessageRepositoryImplTest"]
+++

## TASK-0021: Implementar repositorios de chat

**ConversationRepositoryImpl:**

`findById(String id)`:
```sql
SELECT id, user_id, created_at, updated_at
FROM conversations WHERE id = ?
```

`save(Conversation conv)`:
```sql
INSERT INTO conversations (id, user_id, created_at, updated_at)
VALUES (?, ?, strftime('%Y-%m-%dT%H:%M:%SZ','now'), strftime('%Y-%m-%dT%H:%M:%SZ','now'))
```

`findOrCreate(String userId, String conversationId)`:
- Si `conversationId` no es null/blank: llamar `findById(conversationId)`.
  Si existe y pertenece a `userId`, retornarlo.
  Si no existe: crear nueva conversacion con ese id (o generar UUID nuevo).
- Si `conversationId` es null: generar `UUID.randomUUID().toString()` y crear.

**MessageRepositoryImpl:**

`save(Message msg)`:
```sql
INSERT INTO messages (conversation_id, role, content)
VALUES (?, ?, ?)
```
Retornar el mensaje con el `id` generado.

`findByConversationId(String convId)`:
```sql
SELECT id, conversation_id, role, content, created_at
FROM messages WHERE conversation_id = ? ORDER BY created_at ASC
```

**Tests:**
- Crear conversacion y recuperarla por id.
- `findOrCreate` con id nulo genera UUID nuevo.
- `findOrCreate` con id existente retorna la misma conversacion.
- Guardar mensajes y recuperarlos ordenados.

---

+++
id             = "TASK-0022"
title          = "Implementar EchoAiGateway"
state          = "todo"
dependencies   = []
expected_files = ["ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/EchoAiGateway.java",
                  "ether-chat-backend-infra-sqlite/src/test/java/dev/rafex/chat/chat/infra/EchoAiGatewayTest.java"]
close_criteria = "EchoAiGateway retorna '[ECHO] <ultimo mensaje>'"
validation     = ["./mvnw test -pl ether-chat-backend-infra-sqlite -Dtest=EchoAiGatewayTest"]
+++

## TASK-0022: Implementar EchoAiGateway

```java
package dev.rafex.chat.chat.infra;

import dev.rafex.chat.chat.domain.Message;
import dev.rafex.chat.chat.port.AiGateway;
import java.util.List;

public final class EchoAiGateway implements AiGateway {
    @Override
    public String generate(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return "[ECHO] (empty history)";
        }
        var last = history.get(history.size() - 1);
        return "[ECHO] " + last.content();
    }
}
```

Tests:
- Historia vacia → `"[ECHO] (empty history)"`.
- Historia con un mensaje `"Hola"` → `"[ECHO] Hola"`.
- Historia con multiples mensajes → repite el ultimo.

---

+++
id             = "TASK-0023"
title          = "Implementar ChatServiceImpl"
state          = "todo"
dependencies   = ["TASK-0021", "TASK-0022"]
expected_files = ["ether-chat-backend-core/src/main/java/dev/rafex/chat/chat/service/ChatServiceImpl.java",
                  "ether-chat-backend-core/src/test/java/dev/rafex/chat/chat/service/ChatServiceImplTest.java"]
close_criteria = "Tests de ChatServiceImpl pasan con mocks"
validation     = ["./mvnw test -pl ether-chat-backend-core -Dtest=ChatServiceImplTest"]
+++

## TASK-0023: Implementar ChatServiceImpl

Crear `ChatResponse` record en core o en common:
```java
public record ChatResponse(String content, String conversationId) {}
```

**ChatService** interface en ports (o core, segun donde tenga mas sentido):
```java
public interface ChatService {
    ChatResponse sendMessage(String userId, String conversationId, String message);
}
```

**ChatServiceImpl:**
```java
public final class ChatServiceImpl implements ChatService {
    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final AiGateway ai;

    public ChatServiceImpl(ConversationRepository convRepo,
                           MessageRepository msgRepo, AiGateway ai) {
        this.convRepo = Objects.requireNonNull(convRepo, "convRepo");
        this.msgRepo  = Objects.requireNonNull(msgRepo,  "msgRepo");
        this.ai       = Objects.requireNonNull(ai,       "ai");
    }

    @Override
    public ChatResponse sendMessage(String userId, String conversationId, String message) {
        Objects.requireNonNull(userId, "userId");
        if (message == null || message.isBlank())
            throw new AppError.BadRequest("message is required");

        var conv    = convRepo.findOrCreate(userId, conversationId);
        var userMsg = msgRepo.save(new Message(null, conv.id(), MessageRole.user, message, null));
        var history = msgRepo.findByConversationId(conv.id());
        var content = ai.generate(history);
        msgRepo.save(new Message(null, conv.id(), MessageRole.agent, content, null));
        return new ChatResponse(content, conv.id());
    }
}
```

**Tests con Mockito:**
- Mensaje con conversationId nulo → crea nueva conversacion y retorna content.
- Mensaje con conversationId existente → reutiliza la conversacion.
- Mensaje nulo o blank → `AppError.BadRequest` lanzado.
- Verifica que AiGateway.generate() se llama con la historia correcta.

---

+++
id             = "TASK-0024"
title          = "Cablear ChatService en AppBootstrap y AppContainer"
state          = "todo"
dependencies   = ["TASK-0023"]
expected_files = ["ether-chat-backend-bootstrap/src/main/java/dev/rafex/chat/bootstrap/AppContainer.java",
                  "ether-chat-backend-bootstrap/src/main/java/dev/rafex/chat/bootstrap/AppBootstrap.java"]
close_criteria = "./mvnw compile -pl ether-chat-backend-bootstrap --also-make pasa"
validation     = ["./mvnw compile -pl ether-chat-backend-bootstrap --also-make"]
+++

## TASK-0024: Cablear ChatService

Actualizar `AppContainer`:
```java
public record AppContainer(
    AuthPort authPort,
    ChatService chatService,
    JsonCodec json
) {}
```

En `AppBootstrap.start()`, despues de crear los repositorios:
```java
AiGateway ai       = new EchoAiGateway();
var chatService    = new ChatServiceImpl(convRepo, msgRepo, ai);
var json           = JsonCodec.create();  // ether-json API
```

Actualizar `AppContainer` con `chatService` y `json`.

---

+++
id             = "TASK-0025"
title          = "Implementar ChatMessageHandler"
state          = "todo"
dependencies   = ["TASK-0024"]
expected_files = ["ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/chat/handler/ChatMessageHandler.java",
                  "ether-chat-backend-transport-jetty/src/test/java/dev/rafex/chat/chat/handler/ChatMessageHandlerTest.java"]
close_criteria = "Tests de ChatMessageHandler pasan con mocks"
validation     = ["./mvnw test -pl ether-chat-backend-transport-jetty -Dtest=ChatMessageHandlerTest"]
+++

## TASK-0025: Implementar ChatMessageHandler

`POST /api/chat/message`:

```java
public final class ChatMessageHandler implements HttpHandler {
    private final ChatService chatService;
    private final JsonCodec json;
    private final ServerConfig config;

    // handle():
    // 1. Verificar POST; si no, 405
    // 2. Extraer "Authorization" header; si ausente, 401 RFC 7807
    // 3. Verificar Bearer prefix; si incorrecto, 401
    // 4. Verificar JWT con ether-jwt usando config.jwtSecret():
    //    var claims = EtherJwt.verify(token, config.jwtSecret());
    //    String userId = claims.get("sub").asText();
    // 5. Leer body JSON:
    //    { "message": "string", "conversation_id": "string|null" }
    // 6. Validar message no blank; si invalido 400
    // 7. chatService.sendMessage(userId, conversationId, message)
    // 8. Responder 200 con {"content":"...","conversation_id":"..."}
    //
    // Excepciones:
    //   JwtException         → 401 RFC 7807 "Token invalid or expired"
    //   AppError.Unauthorized → 401 RFC 7807
    //   AppError.BadRequest  → 400 RFC 7807
}
```

Registrar en `AppServer.java`:
```java
.handler("/api/chat/message", new ChatMessageHandler(container.chatService(),
                                                      container.json(),
                                                      AppConfig.load().server()))
```

**Tests con Mockito:**
- JWT valido + body correcto → 200 con `{"content":"[ECHO] Hola","conversation_id":"<uuid>"}`.
- Sin Authorization header → 401.
- JWT invalido → 401.
- Body sin `message` → 400.

---

+++
id             = "TASK-0026"
title          = "Validacion end-to-end de chat-service"
state          = "todo"
dependencies   = ["TASK-0025"]
expected_files = []
close_criteria = "Flujo completo: login → chat funciona end-to-end"
validation     = ["./mvnw clean verify",
                  "flujo completo con curl"]
+++

## TASK-0026: Validacion e2e chat

1. `./mvnw clean verify` — BUILD SUCCESS.
2. Arrancar el jar.
3. Obtener token:
   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"demo","password":"password123"}' | jq -r .token)
   ```
4. Enviar mensaje:
   ```bash
   curl -X POST http://localhost:8080/api/chat/message \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d '{"message":"Hola","conversation_id":null}'
   ```
   Debe responder:
   ```json
   {"content":"[ECHO] Hola","conversation_id":"<uuid>"}
   ```
5. Segundo mensaje con el mismo `conversation_id`:
   ```bash
   CONV_ID="<uuid-del-paso-anterior>"
   curl -X POST http://localhost:8080/api/chat/message \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d "{\"message\":\"Segunda pregunta\",\"conversation_id\":\"$CONV_ID\"}"
   ```
   Debe retornar el mismo `conversation_id`.

6. Sin token → 401.

7. Verificar que el frontend `ether-chat-web` puede conectarse:
   ```bash
   VITE_BACKEND_URL=http://localhost:8080 just dev
   ```
   Login con `demo/password123` y envio de mensajes debe funcionar.

8. Actualizar SPEC-0003 `state = "done"`.
9. Actualizar TRACEABILITY.md con resultado final.
