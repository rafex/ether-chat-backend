+++
artifact_type = "spec"
id            = "SPEC-0002"
state         = "draft"
owner         = "rafex"
created_at    = "2026-04-17"
updated_at    = "2026-04-17"
replaces      = "none"
related_tasks       = ["TASK-0010", "TASK-0011", "TASK-0012", "TASK-0013",
                       "TASK-0014", "TASK-0015"]
related_decisions   = ["DEC-0001", "DEC-0005", "DEC-0006"]
artifacts           = ["ether-chat-backend-ports/src/main/java/dev/rafex/chat/auth/port/",
                       "ether-chat-backend-core/src/main/java/dev/rafex/chat/auth/",
                       "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/auth/infra/",
                       "ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/auth/handler/"]
validation          = ["./mvnw test -pl ether-chat-backend-core",
                       "curl POST /api/auth/login retorna 200 con token"]
+++

# SPEC-0002: auth-service

## Resumen

Implementar el endpoint `POST /api/auth/login` con persistencia en
`auth.db` (SQLite WAL), hash de contrasena con `ether-crypto` y emision
de JWT con `ether-jwt`.

## Problema

El frontend envia credenciales de usuario y espera recibir un JWT para
autenticar las siguientes peticiones. Sin este endpoint el chat no puede
funcionar en modo autenticado.

## Objetivo

`POST /api/auth/login` funciona end-to-end: valida credenciales contra
`auth.db`, retorna un JWT firmado con HS256, y los tests unitarios pasan.
Un usuario de seed debe existir al arrancar la aplicacion.

## Alcance

**Incluye:**
- Schema SQL para tabla `users` en `auth.db`
- `AuthDb.init()`: aplica schema y pragmas WAL al arrancar
- `User` record con `id`, `username`, `passwordHash`, `createdAt`
- `Credentials` record con `username`, `password`
- `Session` record con `token`, `expiresAt`
- `UserRepository` interface: `findByUsername(String)`, `save(User)`
- `AuthPort` interface: `login(Credentials): Session`
- `AuthServiceImpl`: implementa `AuthPort`, usa `UserRepository` +
  `ether-crypto` para verificar hash + `ether-jwt` para emitir token
- `UserRepositoryImpl`: implementa `UserRepository` con `DatabaseClient`
  sobre `auth.db`
- `LoginHandler`: maneja `POST /api/auth/login`, llama `AuthPort`,
  retorna `{token}` o RFC 7807 error
- Seed de usuario `demo/password123` al arrancar si no existe
- Tests unitarios de `AuthServiceImpl` con mock de `UserRepository`
- Tests de `UserRepositoryImpl` con SQLite en memoria

**Excluye:**
- Registro de nuevos usuarios via API
- Logout (el token expira por tiempo)
- Refresh de token
- Roles o permisos

## Requisitos funcionales

- RF-1: `POST /api/auth/login` con `{"username":"demo","password":"password123"}`
  retorna `200` con `{"token":"<jwt>"}`.
- RF-2: `POST /api/auth/login` con contrasena incorrecta retorna `401`
  con body RFC 7807.
- RF-3: `POST /api/auth/login` sin body o sin campos retorna `400`
  con body RFC 7807.
- RF-4: El JWT puede verificarse con `ether-jwt` usando el mismo
  `JWT_SECRET` configurado.
- RF-5: El JWT contiene el claim `sub` con el `username`.
- RF-6: `AuthDb.init()` ejecuta los pragmas WAL y crea la tabla
  `users` con `CREATE TABLE IF NOT EXISTS`.
- RF-7: Al arrancar, si no existe el usuario `demo`, se crea con
  hash de `password123`.

## Schema SQL

```sql
CREATE TABLE IF NOT EXISTS users (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    username     TEXT    NOT NULL UNIQUE,
    password_hash TEXT   NOT NULL,
    created_at   TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
);
```

## Pragmas WAL (auth.db)

```sql
PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA cache_size=10000;
PRAGMA temp_store=MEMORY;
PRAGMA foreign_keys=ON;
```

## Requisitos no funcionales

- RNF-1: La contrasena nunca se almacena en claro; usar
  `ether-crypto` para hash y verificacion.
- RNF-2: El JWT expira en `JWT_EXPIRY_SECONDS` (default 86400).
- RNF-3: `LoginHandler` no tiene logica de negocio; solo traduce
  HTTP ↔ dominio.

## Criterios de aceptacion

- Dado el backend arrancado con `JWT_SECRET` y `AUTH_DB_PATH`
- Cuando `POST /api/auth/login {"username":"demo","password":"password123"}`
- Entonces responde `200` con `{"token":"<string>"}`

- Dado el mismo backend
- Cuando `POST /api/auth/login {"username":"demo","password":"wrong"}`
- Entonces responde `401` con body RFC 7807

- Dado el JWT retornado
- Cuando se decodifica el claim `sub`
- Entonces el valor es `"demo"`

## Dependencias

- SPEC-0001 debe estar en estado `done`: el proyecto Maven compila.

## Clases clave a implementar

### `AuthDb.java` (infra-sqlite)
```java
package dev.rafex.chat.auth.infra;

import dev.rafex.ether.database.core.DatabaseClient;

public final class AuthDb {
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
                created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
            )
        """);
    }
}
```

### `AuthServiceImpl.java` (core)
```java
// Dependencias: UserRepository (port), ether-crypto (hash), ether-jwt (token)
// Flujo login():
//   1. repository.findByUsername(credentials.username()) → Optional<User>
//   2. Si vacio → throw AppError (unauthorized)
//   3. EtherCrypto.verify(credentials.password(), user.passwordHash()) → bool
//   4. Si false → throw AppError (unauthorized)
//   5. EtherJwt.issue(Map.of("sub", username), expirySeconds) → token string
//   6. return new Session(token, expiresAt)
```

### `LoginHandler.java` (transport-jetty)
```java
// POST /api/auth/login
// Lee body JSON con ether-json → Credentials
// Llama authPort.login(credentials)
// Retorna JSON {"token": "..."} con status 200
// En AppError (unauthorized) → ether-http-problem, status 401
// En IllegalArgumentException → ether-http-problem, status 400
```
