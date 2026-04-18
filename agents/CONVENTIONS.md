# CONVENTIONS.md

Reglas de codigo y organizacion del proyecto.

## Java

- Java 21: usar records, sealed classes y pattern matching donde aplique.
- No usar reflexion en codigo de produccion.
- No usar anotaciones de framework (no `@Component`, `@Inject`, `@Autowired`).
- Clases utilitarias: constructor privado, metodos estaticos, `final`.
- Implementaciones de servicios: clase `final` con constructor explicito.
- Registrar con `java.util.logging` (JUL), no con SLF4J directamente.
  `ether-http-jetty12` ya configura el bridge SLF4J → JUL.
- Preferir `Objects.requireNonNull(param, "param")` en constructores.
- Usar `Optional<T>` solo en retornos de repositorios, no en parametros.

## Naming

| Elemento | Convencion | Ejemplo |
|----------|-----------|---------|
| Clases de dominio | PascalCase | `User`, `Conversation`, `Message` |
| Records de config | PascalCase + sufijo Config | `AppConfig`, `ServerConfig` |
| Interfaces de puerto | PascalCase sin prefijo I | `UserRepository`, `AiGateway` |
| Implementaciones de puerto | sufijo Impl | `UserRepositoryImpl` |
| Handlers HTTP | sufijo Handler | `LoginHandler`, `ChatMessageHandler` |
| Inicializadores de DB | sufijo Db | `AuthDb`, `ChatDb` |
| Packages | lowercase, bounded context primero | `dev.rafex.chat.auth.service` |
| Modulos Maven | kebab-case con prefijo del proyecto | `ether-chat-backend-core` |
| Variables de entorno | UPPER_SNAKE_CASE | `SERVER_PORT`, `JWT_SECRET` |

## Estructura de packages por modulo

```
modulo-common:
  dev.rafex.chat.shared.config.*
  dev.rafex.chat.shared.error.*

modulo-ports:
  dev.rafex.chat.auth.port.*
  dev.rafex.chat.chat.port.*

modulo-core:
  dev.rafex.chat.auth.domain.*
  dev.rafex.chat.auth.service.*
  dev.rafex.chat.chat.domain.*
  dev.rafex.chat.chat.service.*

modulo-infra-sqlite:
  dev.rafex.chat.auth.infra.*
  dev.rafex.chat.chat.infra.*

modulo-bootstrap:
  dev.rafex.chat.bootstrap.*

modulo-transport-jetty:
  dev.rafex.chat            (App.java)
  dev.rafex.chat.server.*
  dev.rafex.chat.auth.handler.*
  dev.rafex.chat.chat.handler.*
```

## Tests

- Un test por clase publica; nombre: `<ClaseTesteada>Test`.
- Tests unitarios en el mismo modulo que el codigo bajo prueba.
- Tests de arquitectura en `architecture-tests` con ArchUnit.
- Cada handler debe tener al menos un test con mock del servicio.
- Cada servicio debe tener tests que ejerciten los caminos criticos.
- Tests de repositorio con SQLite real en memoria (`jdbc:sqlite::memory:`).

## Maven

- Cada modulo declara solo sus dependencias directas.
- Las versiones se gestionan en el `dependencyManagement` del parent.
- No usar `<scope>compile</scope>` explicito (es el default).
- `ether-glowroot-jetty12` siempre se declara con `<scope>provided</scope>`
  en el parent y como runtime dependency en `transport-jetty`.

## Branching y commits

- Rama principal: `main`
- Feature branches: `feat/<iniciativa>/<descripcion-corta>`
- Commits: imperativo presente, en ingles o espanol.
  Ejemplo: `feat: add LoginHandler for POST /api/auth/login`
- No mezclar refactors con funcionalidad en el mismo commit.

## Documentacion SpecNative

- Antes de editar codigo, leer la spec y las tareas asociadas.
- Actualizar estado de tarea en `tasks/` al terminar cada una.
- No cerrar una tarea sin evidencia de validacion (output de test o curl).
- Si una decision cambia, actualizar `DECISIONS.md` antes de seguir.
