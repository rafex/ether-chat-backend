+++
artifact_type = "task_file"
initiative    = "auth-service"
spec_id       = "SPEC-0002"
owner         = "rafex"
state         = "todo"
+++

# Tasks: auth-service

Tareas para implementar `POST /api/auth/login`. Requiere SPEC-0001 done.

---

+++
id             = "TASK-0010"
title          = "Implementar AuthDb.init() con schema SQL y pragmas WAL"
state          = "todo"
dependencies   = []
expected_files = ["ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/auth/infra/AuthDb.java"]
close_criteria = "AuthDb.init() con DatabaseClient en memoria no lanza excepciones"
validation     = ["./mvnw test -pl ether-chat-backend-infra-sqlite -Dtest=AuthDbTest"]
+++

## TASK-0010: Implementar AuthDb

Completar `AuthDb.init(DatabaseClient db)` con:
1. Los 5 pragmas WAL (journal_mode, synchronous, cache_size, temp_store, foreign_keys).
2. `CREATE TABLE IF NOT EXISTS users (...)` con el schema de SPEC-0002.

Escribir `AuthDbTest.java` que:
- Abre conexion SQLite en memoria: `jdbc:sqlite::memory:`
- Llama `AuthDb.init(db)` sin lanzar excepciones
- Verifica que la tabla `users` existe con `SELECT name FROM sqlite_master WHERE type='table'`

---

+++
id             = "TASK-0011"
title          = "Implementar UserRepositoryImpl"
state          = "todo"
dependencies   = ["TASK-0010"]
expected_files = ["ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/auth/infra/UserRepositoryImpl.java",
                  "ether-chat-backend-infra-sqlite/src/test/java/dev/rafex/chat/auth/infra/UserRepositoryImplTest.java"]
close_criteria = "Tests de UserRepositoryImpl pasan con SQLite en memoria"
validation     = ["./mvnw test -pl ether-chat-backend-infra-sqlite -Dtest=UserRepositoryImplTest"]
+++

## TASK-0011: Implementar UserRepositoryImpl

**findByUsername(String username):**
```sql
SELECT id, username, password_hash, created_at
FROM users WHERE username = ?
```
Mapear fila a `User` record. Retornar `Optional.empty()` si no existe.

**save(User user):**
```sql
INSERT INTO users (username, password_hash) VALUES (?, ?)
```
Recuperar el `id` generado (GENERATED KEYS o last_insert_rowid()).
Retornar `new User(id, username, passwordHash, createdAt)`.

**Tests con SQLite en memoria:**
- Guardar un usuario y recuperarlo por username.
- Buscar un username inexistente retorna `Optional.empty()`.
- Guardar dos usuarios con el mismo username lanza excepcion SQL.

---

+++
id             = "TASK-0012"
title          = "Implementar AuthServiceImpl con ether-crypto y ether-jwt"
state          = "todo"
dependencies   = ["TASK-0011"]
expected_files = ["ether-chat-backend-core/src/main/java/dev/rafex/chat/auth/service/AuthServiceImpl.java",
                  "ether-chat-backend-core/src/test/java/dev/rafex/chat/auth/service/AuthServiceImplTest.java"]
close_criteria = "Tests de AuthServiceImpl pasan con mock de UserRepository"
validation     = ["./mvnw test -pl ether-chat-backend-core -Dtest=AuthServiceImplTest"]
+++

## TASK-0012: Implementar AuthServiceImpl

Constructor:
```java
public AuthServiceImpl(UserRepository repository, ServerConfig config) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.config = Objects.requireNonNull(config, "config");
}
```

Metodo `login(Credentials credentials)`:
1. Validar que username y password no son nulos/blank; si son invalidos lanzar
   `AppError.BadRequest("username and password required")`.
2. `repository.findByUsername(credentials.username())` → si vacio lanzar
   `AppError.Unauthorized("Invalid credentials")`.
3. Verificar hash con `ether-crypto` API (consultar javadoc; probablemente
   `EtherCrypto.verifyPassword(raw, hash)` o similar).
4. Si verificacion falla: `throw new AppError.Unauthorized("Invalid credentials")`.
5. Emitir JWT con `ether-jwt`:
   ```java
   // Ejemplo orientativo; adaptar a la API real de ether-jwt:
   var token = EtherJwt.builder()
       .secret(config.jwtSecret())
       .claim("sub", user.username())
       .expiresInSeconds(config.jwtExpirySeconds())
       .sign();
   ```
6. Calcular `expiresAt` como `Instant.now().plusSeconds(config.jwtExpirySeconds())`.
7. Retornar `new Session(token, expiresAt.toString())`.

**Tests con Mockito:**
- Login exitoso: mock retorna usuario con hash valido → Session con token no nulo.
- Username no encontrado → `AppError.Unauthorized` lanzado.
- Contrasena incorrecta → `AppError.Unauthorized` lanzado.
- Campos vacios → `AppError.BadRequest` lanzado.

---

+++
id             = "TASK-0013"
title          = "Seed de usuario demo en AppBootstrap"
state          = "todo"
dependencies   = ["TASK-0012"]
expected_files = ["ether-chat-backend-bootstrap/src/main/java/dev/rafex/chat/bootstrap/SeedUsers.java"]
close_criteria = "Al arrancar, si auth.db esta vacia, el usuario demo existe"
validation     = ["manual: arrancar app y verificar login con demo/password123"]
+++

## TASK-0013: Seed de usuario demo

Crear `SeedUsers.java` en bootstrap:
```java
public final class SeedUsers {
    private SeedUsers() {}

    public static void seedIfAbsent(UserRepository repo, ServerConfig config) {
        if (repo.findByUsername("demo").isEmpty()) {
            var hash = EtherCrypto.hashPassword("password123");
            repo.save(new User(null, "demo", hash, null));
        }
    }
}
```

Llamar `SeedUsers.seedIfAbsent(userRepo, config.server())` en `AppBootstrap.start()`
despues de `AuthDb.init(authDb)`.

---

+++
id             = "TASK-0014"
title          = "Implementar LoginHandler"
state          = "todo"
dependencies   = ["TASK-0013"]
expected_files = ["ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/auth/handler/LoginHandler.java",
                  "ether-chat-backend-transport-jetty/src/test/java/dev/rafex/chat/auth/handler/LoginHandlerTest.java"]
close_criteria = "Tests de LoginHandler pasan con mock de AuthPort"
validation     = ["./mvnw test -pl ether-chat-backend-transport-jetty -Dtest=LoginHandlerTest"]
+++

## TASK-0014: Implementar LoginHandler

`LoginHandler` implementa el handler de `ether-http-jetty12` para
`POST /api/auth/login`:

```java
public final class LoginHandler implements HttpHandler {   // interfaz de ether-http-core
    private final AuthPort authPort;
    private final JsonCodec json;   // ether-json

    public LoginHandler(AuthPort authPort, JsonCodec json) { ... }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 1. Verificar metodo POST; si no, 405
        // 2. Leer body como String
        // 3. Deserializar a Credentials con json.decode(body, Credentials.class)
        // 4. Validar campos; si nulos lanzar AppError.BadRequest
        // 5. authPort.login(credentials) → Session
        // 6. Responder 200 con json.encode(Map.of("token", session.token()))
        // En AppError.Unauthorized → ether-http-problem, 401
        // En AppError.BadRequest → ether-http-problem, 400
    }
}
```

Registrar en `AppServer.java`:
```java
.handler("/api/auth/login", new LoginHandler(container.authPort(), json))
```

**Tests con Mockito:**
- Peticion valida → 200 con `{"token":"..."}`.
- Contrasena incorrecta (mock lanza Unauthorized) → 401.
- Body sin campos → 400.

---

+++
id             = "TASK-0015"
title          = "Validacion end-to-end de auth-service"
state          = "todo"
dependencies   = ["TASK-0014"]
expected_files = []
close_criteria = "POST /api/auth/login retorna JWT verificable"
validation     = ["./mvnw clean verify",
                  "curl -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{\"username\":\"demo\",\"password\":\"password123\"}'"]
+++

## TASK-0015: Validacion e2e auth

1. `./mvnw clean verify` — BUILD SUCCESS.
2. Arrancar el jar:
   ```bash
   JWT_SECRET=test-secret-minimum-32-characters! \
   java -jar ether-chat-backend-transport-jetty/target/*-jar-with-dependencies.jar
   ```
3. Ejecutar:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"demo","password":"password123"}'
   ```
   Debe responder `{"token":"<jwt_string>"}` con status 200.

4. Ejecutar con credenciales incorrectas:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"demo","password":"wrong"}'
   ```
   Debe responder `401` con body RFC 7807.

5. Actualizar SPEC-0002 `state = "done"`.
6. Actualizar SPEC-0003 `state = "active"`.
7. Actualizar TRACEABILITY.md.
