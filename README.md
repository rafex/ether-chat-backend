# ether-chat-backend

Backend del sistema de chat de agentes IA. Java 21, arquitectura hexagonal,
Ether Framework, SQLite WAL. Sin frameworks pesados.

## Navegacion

| Ruta | Contenido |
|------|-----------|
| `agents/README.md` | Indice del contexto operativo para agentes |
| `agents/PRODUCT.md` | Problema, usuarios y objetivos del producto |
| `agents/STACK.md` | Stack tecnologico, versiones y coordenadas Maven |
| `agents/ARCHITECTURE.md` | Arquitectura hexagonal, modulos y dependencias |
| `agents/DECISIONS.md` | Decisiones persistentes del proyecto |
| `agents/SPEC.md` | Punto de entrada a las specs activas |
| `agents/ROADMAP.md` | Orden de implementacion y prioridades |
| `agents/CONVENTIONS.md` | Convenciones de codigo Java |
| `agents/COMMANDS.md` | Comandos Maven de build, test y run |
| `agents/TRACEABILITY.md` | Mapa de specs, tareas y artefactos |
| `agents/specs/` | Specs por iniciativa |
| `tasks/` | Tareas ejecutables por iniciativa |
| `openapi/openapi.yaml` | Contrato OpenAPI 3.0 para importar en Postman |

## Inicio rapido

```bash
# Compilar todo desde la raiz
./mvnw clean package -DskipTests

# Ejecutar (puerto por default 8080)
java -jar ether-chat-backend-transport-jetty/target/ether-chat-backend-transport-jetty-jar-with-dependencies.jar
```

## Variables de entorno

```bash
SERVER_PORT=8080
AUTH_DB_PATH=./data/auth.db
CHAT_DB_PATH=./data/chat.db
JWT_SECRET=supersecret-minimum-32-characters!
JWT_EXPIRY_SECONDS=86400
AI_PROVIDER=echo
DEEPSEEK_API_KEY=
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
```

`JWT_SECRET` es requerido para login y endpoints protegidos.
`AI_PROVIDER` soporta `echo|deepseek`.
Si `AI_PROVIDER=deepseek` y falta `DEEPSEEK_API_KEY`, el backend hace fallback a `EchoAiGateway`.

## Usar la API (curl)

```bash
# 1) Health
curl http://localhost:8080/health

# 2) Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"password123"}' | jq -r '.token')

# 3) Chat (Authorization debe ser Bearer <token>)
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"message":"Hola","conversation_id":null}'
```

## OpenAPI y Postman

1. Importa [`openapi/openapi.yaml`](./openapi/openapi.yaml) en Postman.
2. Define la variable `baseUrl` con `http://localhost:8080`.
3. Ejecuta primero `POST /api/auth/login` y usa el `token`.
4. Para `POST /api/chat/message`, envía `Authorization: Bearer <token>`.

`8080` — configurable con `SERVER_PORT`.
