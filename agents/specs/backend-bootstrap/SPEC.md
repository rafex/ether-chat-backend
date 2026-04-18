+++
artifact_type = "spec"
id            = "SPEC-0001"
state         = "active"
owner         = "rafex"
created_at    = "2026-04-17"
updated_at    = "2026-04-17"
replaces      = "none"
related_tasks       = ["TASK-0001", "TASK-0002", "TASK-0003", "TASK-0004",
                       "TASK-0005", "TASK-0006", "TASK-0007", "TASK-0008"]
related_decisions   = ["DEC-0002", "DEC-0003", "DEC-0004"]
artifacts           = ["pom.xml", "ether-chat-backend-*/pom.xml",
                       "ether-chat-backend-*/.mvn/wrapper/maven-wrapper.properties"]
validation          = ["./mvnw clean compile"]
+++

# SPEC-0001: backend-bootstrap

## Resumen

Crear la estructura Maven multi-modulo completa del backend, siguiendo el
patron del `ether-archetype`, con SQLite como motor de base de datos. Al
terminar esta spec debe ser posible compilar el proyecto completo con
`./mvnw clean compile` desde la raiz.

## Problema

No existe aun ningun archivo de proyecto. El agente debe crear toda la
estructura desde cero antes de poder implementar funcionalidad.

## Objetivo

El proyecto Maven multi-modulo con 7 modulos compila sin errores. Los
packages existen y las restricciones de dependencias entre capas estan
configuradas.

## Alcance

**Incluye:**
- `pom.xml` raiz con parent de Ether y declaracion de todos los modulos
- 7 modulos Maven con sus `pom.xml` y packages base
- Maven Wrapper en el root y en cada submodulo
- Clases esqueleto (vacia, compilable) en cada modulo
- `AppConfig`, `DatabaseConfig`, `ServerConfig` con lectura de env vars
- `AppError` sealed y `ChatErrorCode` enum
- `AppBootstrap`, `AppContainer` (sin implementaciones reales aun)
- `App.java` con `main()` que arranca el server
- `HealthHandler` que responde `{"status":"ok"}`
- Test de arquitectura ArchUnit esqueleto

**Excluye:**
- Logica de auth o chat (cubierta en SPEC-0002, SPEC-0003)
- Inicializacion de SQLite (se hace en SPEC-0002 para auth.db y SPEC-0003 para chat.db)
- Tests funcionales de endpoints

## Requisitos funcionales

- RF-1: El proyecto tiene un `pom.xml` raiz con `<packaging>pom</packaging>` que
  declara los 7 modulos como `<module>`.
- RF-2: Cada modulo tiene su propio `pom.xml` con parent apuntando al root.
- RF-3: Las dependencias entre modulos siguen el grafo definido en `ARCHITECTURE.md`.
  El compilador debe rechazar cualquier dependencia prohibida.
- RF-4: El root `pom.xml` declara todas las dependencias de Ether en
  `<dependencyManagement>` con sus coordenadas exactas.
- RF-5: Cada modulo tiene su propio Maven Wrapper funcional.
- RF-6: `App.java` tiene un `main()` que arranca Jetty en `SERVER_PORT`.
- RF-7: `GET /health` responde `{"status":"ok","version":"1.0.0-SNAPSHOT","timestamp":"<ISO>"}`
- RF-8: `AppConfig` lee `SERVER_PORT`, `SERVER_SANDBOX`, `AUTH_DB_PATH`,
  `CHAT_DB_PATH`, `JWT_SECRET`, `JWT_EXPIRY_SECONDS` desde variables de entorno.

## Requisitos no funcionales

- RNF-1: Java 21. El parent sobreescribe la version de Java del `ether-parent`
  (que por defecto es 25) con `<java.version>21</java.version>`.
- RNF-2: `./mvnw clean compile` no produce warnings de compilacion.
- RNF-3: Maven Wrapper version: `3.9.9`.

## Estructura exacta de modulos a crear

```
ether-chat-backend/
  pom.xml
  mvnw  mvnw.cmd  .mvn/wrapper/maven-wrapper.properties
  ether-chat-backend-common/
    pom.xml
    mvnw  mvnw.cmd  .mvn/wrapper/maven-wrapper.properties
    src/main/java/dev/rafex/chat/shared/config/AppConfig.java
    src/main/java/dev/rafex/chat/shared/config/DatabaseConfig.java
    src/main/java/dev/rafex/chat/shared/config/ServerConfig.java
    src/main/java/dev/rafex/chat/shared/error/AppError.java
    src/main/java/dev/rafex/chat/shared/error/ChatErrorCode.java
  ether-chat-backend-ports/
    pom.xml
    mvnw  mvnw.cmd  .mvn/wrapper/maven-wrapper.properties
    src/main/java/dev/rafex/chat/auth/port/UserRepository.java
    src/main/java/dev/rafex/chat/auth/port/AuthPort.java
    src/main/java/dev/rafex/chat/chat/port/ConversationRepository.java
    src/main/java/dev/rafex/chat/chat/port/MessageRepository.java
    src/main/java/dev/rafex/chat/chat/port/AiGateway.java
  ether-chat-backend-core/
    pom.xml
    mvnw  mvnw.cmd  .mvn/wrapper/maven-wrapper.properties
    src/main/java/dev/rafex/chat/auth/domain/User.java
    src/main/java/dev/rafex/chat/auth/domain/Credentials.java
    src/main/java/dev/rafex/chat/auth/domain/Session.java
    src/main/java/dev/rafex/chat/auth/service/AuthService.java
    src/main/java/dev/rafex/chat/auth/service/AuthServiceImpl.java
    src/main/java/dev/rafex/chat/chat/domain/Conversation.java
    src/main/java/dev/rafex/chat/chat/domain/Message.java
    src/main/java/dev/rafex/chat/chat/domain/MessageRole.java
    src/main/java/dev/rafex/chat/chat/service/ChatService.java
    src/main/java/dev/rafex/chat/chat/service/ChatServiceImpl.java
  ether-chat-backend-infra-sqlite/
    pom.xml
    mvnw  mvnw.cmd  .mvn/wrapper/maven-wrapper.properties
    src/main/java/dev/rafex/chat/auth/infra/AuthDb.java
    src/main/java/dev/rafex/chat/auth/infra/UserRepositoryImpl.java
    src/main/java/dev/rafex/chat/chat/infra/ChatDb.java
    src/main/java/dev/rafex/chat/chat/infra/ConversationRepositoryImpl.java
    src/main/java/dev/rafex/chat/chat/infra/MessageRepositoryImpl.java
    src/main/java/dev/rafex/chat/chat/infra/EchoAiGateway.java
  ether-chat-backend-bootstrap/
    pom.xml
    mvnw  mvnw.cmd  .mvn/wrapper/maven-wrapper.properties
    src/main/java/dev/rafex/chat/bootstrap/AppContainer.java
    src/main/java/dev/rafex/chat/bootstrap/AppBootstrap.java
  ether-chat-backend-transport-jetty/
    pom.xml
    mvnw  mvnw.cmd  .mvn/wrapper/maven-wrapper.properties
    src/main/java/dev/rafex/chat/App.java
    src/main/java/dev/rafex/chat/server/AppServer.java
    src/main/java/dev/rafex/chat/shared/handler/HealthHandler.java
  ether-chat-backend-architecture-tests/
    pom.xml
    mvnw  mvnw.cmd  .mvn/wrapper/maven-wrapper.properties
    src/test/java/dev/rafex/chat/arch/HexagonalArchitectureTest.java
```

## Coordenadas Maven del proyecto

```xml
groupId:    dev.rafex.chat
artifactId: ether-chat-backend  (root parent)
version:    1.0.0-SNAPSHOT
```

## Coordenadas del parent de Ether

```xml
<parent>
    <groupId>dev.rafex.ether.parent</groupId>
    <artifactId>ether-parent</artifactId>
    <version>9.5.5</version>
</parent>
```

Verificar la ultima version estable en:
`https://central.sonatype.com/artifact/dev.rafex.ether.parent/ether-parent`

## Criterios de aceptacion

- Dado el proyecto recien clonado
- Cuando se ejecuta `./mvnw clean compile` desde la raiz
- Entonces la salida es `BUILD SUCCESS` con 7 modulos compilados

- Dado el fat-jar generado con `./mvnw package -DskipTests`
- Cuando se ejecuta el jar con `JWT_SECRET=test-secret-32-chars-minimum`
- Entonces `curl http://localhost:8080/health` retorna `{"status":"ok",...}`

## Dependencias

Ninguna. Es la primera spec.

## Riesgos

- La version de `ether-parent` en Maven Central puede diferir de la
  usada en el deployment hub local (9.5.5-SNAPSHOT). Verificar y usar
  la ultima version estable publicada.
