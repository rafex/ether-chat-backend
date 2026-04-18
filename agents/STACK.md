# STACK.md

Fuente de verdad de la base tecnologica del proyecto.

## Runtime

- Lenguaje: Java 21 (LTS)
- JVM: OpenJDK 21+
- Build: Maven (Maven Wrapper en cada modulo)

## Ether Framework

Dependencias de Ether. Verificar ultima version estable en:
`https://central.sonatype.com/artifact/dev.rafex.ether.parent/ether-parent`

Propiedad Maven en el parent del proyecto: `<ether.version>9.5.5</ether.version>`

| artifactId | groupId | Uso |
|-----------|---------|-----|
| `ether-di` | `dev.rafex.ether.di` | DI explicito: `Lazy<T>`, `Closer`, bootstrap sin reflexion |
| `ether-config` | `dev.rafex.ether.config` | Config tipada desde env vars y system properties |
| `ether-crypto` | `dev.rafex.ether.crypto` | Hash de contrasenas (bcrypt-like, sin librerias externas) |
| `ether-jdbc` | `dev.rafex.ether.jdbc` | `DatabaseClient` JDBC sin ORM; trae `ether-database-core` |
| `ether-json` | `dev.rafex.ether.json` | Codec JSON basado en Jackson |
| `ether-jwt` | `dev.rafex.ether.jwt` | Emision y verificacion JWT (HS256, RS256, ES256) |
| `ether-observability-core` | `dev.rafex.ether.observability` | RequestId, health probes, timing |
| `ether-http-problem` | `dev.rafex.ether.http` | RFC 7807 Problem Details para errores HTTP |
| `ether-http-jetty12` | `dev.rafex.ether.http` | Servidor HTTP embebido Jetty 12 (`EtherServer`) |
| `ether-websocket-jetty12` | `dev.rafex.ether.websocket` | Endpoints WebSocket sobre Jetty 12 |
| `ether-glowroot-jetty12` | `dev.rafex.ether.glowroot` | APM Glowroot para HTTP y WebSocket (scope provided) |

## Dependencias externas clave

| artifactId | groupId | Version | Uso |
|-----------|---------|---------|-----|
| `sqlite-jdbc` | `org.xerial` | `3.49.1.0` | Driver JDBC para SQLite |
| `archunit-junit5` | `com.tngtech.archunit` | `1.4.1` | Tests de arquitectura hexagonal |
| `junit-jupiter` | `org.junit.jupiter` | gestionado por ether-parent BOM | Tests unitarios |
| `mockito-core` | `org.mockito` | gestionado por ether-parent BOM | Mocks en tests |
| `assertj-core` | `org.assertj` | gestionado por ether-parent BOM | Aserciones fluidas |

## Base de datos

- Motor: SQLite 3 (embebido, sin proceso externo)
- Modo: WAL (Write-Ahead Logging) para escritura intensiva concurrente
- Una base de datos por bounded context:
  - `auth.db` — usuarios y sesiones
  - `chat.db` — conversaciones y mensajes
- Path configurable por variable de entorno

## Servidor HTTP

- Jetty 12 embebido via `ether-http-jetty12`
- Puerto por defecto: `8080` (env var `SERVER_PORT`)
- CORS habilitado para todos los origenes en modo sandbox
- WebSocket via `ether-websocket-jetty12` (para streaming futuro)

## Observabilidad

- APM: Glowroot (`ether-glowroot-jetty12`) cargado como javaagent
- Health check: `GET /health`
- Request ID en cada respuesta via `ether-observability-core`

## CI/CD

- GitHub Actions (ver `pipelines/CI.md`)
- Build: `./mvnw clean verify`
- Java: OpenJDK 21

## Restricciones

- Java 21 minimo; no compilar con versiones menores.
- Sin frameworks pesados (no Spring, no Quarkus, no Micronaut).
- Sin reflection-based DI (no Guice, no CDI).
- SQLite solo; no cambiar a otro motor sin crear un nuevo modulo infra.
- Cada modulo Maven debe tener su propio Maven Wrapper.
- El modulo `ether-parent` de Ether usa Java 25 por defecto; el proyecto
  sobreescribe con `<java.version>21</java.version>` en el parent propio.
