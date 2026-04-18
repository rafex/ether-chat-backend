# DECISIONS.md

Registro de decisiones persistentes del proyecto.

---

### DEC-0001 - SQLite con WAL como motor de base de datos

- Fecha: 2026-04-17
- Estado: `accepted`
- Relacionado con specs: SPEC-0001, SPEC-0002, SPEC-0003
- Contexto: el backend necesita persistencia sin infraestructura externa
  para facilitar el desarrollo local y el despliegue en entornos simples
  (VPS, contenedor sin sidecar de DB).
- Decision: usar SQLite con `journal_mode=WAL` para soportar lecturas
  concurrentes y escritura intensiva sin degradar el rendimiento.
  Cada bounded context (auth, chat) tiene su propio archivo `.db`.
- Consecuencias: maximo un escritor concurrente por archivo; para cargas
  de escritura muy altas se deberá migrar a un motor diferente creando
  un nuevo modulo `infra-postgres` sin modificar `core` ni `ports`.
- Reemplaza: `none`

---

### DEC-0002 - Arquitectura hexagonal con modulos Maven separados

- Fecha: 2026-04-17
- Estado: `accepted`
- Relacionado con specs: SPEC-0001
- Contexto: se necesita que el codigo sea portatil y que los adaptadores
  (DB, HTTP, IA) sean intercambiables sin recompilar el dominio.
- Decision: seguir el patron del `ether-archetype` con modulos Maven
  independientes por capa (`ports`, `core`, `infra-sqlite`, `bootstrap`,
  `transport-jetty`). El grafo de dependencias prohibe que `core` o
  `ports` conozcan infra o transporte.
- Consecuencias: mas modulos Maven que un proyecto monolitico, pero
  las dependencias quedan auditadas por el compilador. ArchUnit valida
  las reglas de capas en `architecture-tests`.
- Reemplaza: `none`

---

### DEC-0003 - Ether Framework sin reflection ni anotaciones

- Fecha: 2026-04-17
- Estado: `accepted`
- Relacionado con specs: SPEC-0001
- Contexto: los frameworks de inyeccion de dependencias basados en
  reflexion (Spring, Guice) agregan startup time, magia y dependencias
  transitivas que oscurecen el codigo.
- Decision: usar `ether-di` con `Lazy<T>` y `Closer` para DI explicita.
  Todos los objetos se construyen en `AppBootstrap`; el grafo de objetos
  es visible en un solo lugar.
- Consecuencias: mas codigo boilerplate en `AppBootstrap`, pero cero
  magia y compatibilidad con GraalVM native-image.
- Reemplaza: `none`

---

### DEC-0004 - Maven Wrapper en cada modulo

- Fecha: 2026-04-17
- Estado: `accepted`
- Relacionado con specs: SPEC-0001
- Contexto: se requiere que cada modulo Maven sea ejecutable de forma
  independiente sin depender de que Maven este instalado en el sistema.
- Decision: cada modulo (root parent y cada submodulo) tiene su propio
  `.mvn/wrapper/maven-wrapper.properties` y sus scripts `mvnw`/`mvnw.cmd`.
  El wrapper de cada submodulo apunta a la misma version de Maven que el
  root para consistencia.
- Consecuencias: mayor cantidad de archivos en el repo, pero cualquier
  modulo puede compilarse de forma autonoma con `./mvnw` local.
- Reemplaza: `none`

---

### DEC-0005 - AiGateway como puerto intercambiable

- Fecha: 2026-04-17
- Estado: `accepted`
- Relacionado con specs: SPEC-0003
- Contexto: el proveedor de IA (OpenAI, Anthropic, Ollama, modelo local)
  debe ser configurable sin recompilar el dominio.
- Decision: `AiGateway` es una interfaz en el modulo `ports`. La
  implementacion por defecto `EchoAiGateway` repite el mensaje del
  usuario. Los integradores implementan `AiGateway` y la inyectan en
  `AppBootstrap`.
- Consecuencias: el backend base no requiere API keys ni acceso a red.
  Un integrador conecta su proveedor de IA implementando ~50 lineas.
- Reemplaza: `none`

---

### DEC-0006 - JWT HS256 para sesiones stateless

- Fecha: 2026-04-17
- Estado: `accepted`
- Relacionado con specs: SPEC-0002
- Contexto: el frontend envia un token en cada peticion. El backend
  debe verificarlo sin consultar la DB en cada request.
- Decision: usar `ether-jwt` con HS256. El secreto se carga de la
  variable de entorno `JWT_SECRET`. No se mantiene lista de tokens
  revocados (stateless). Expiracion configurable via `JWT_EXPIRY_SECONDS`.
- Consecuencias: no hay logout real (el token expira por tiempo). Para
  logout inmediato seria necesaria una lista negra en DB.
- Reemplaza: `none`
