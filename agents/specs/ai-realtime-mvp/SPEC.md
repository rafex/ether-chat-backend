+++
artifact_type = "spec"
id            = "SPEC-0004"
state         = "active"
owner         = "rafex"
created_at    = "2026-04-18"
updated_at    = "2026-04-18"
replaces      = "none"
related_tasks       = ["TASK-0030", "TASK-0031", "TASK-0032", "TASK-0033", "TASK-0034", "TASK-0035", "TASK-0036", "TASK-0037", "TASK-0038"]
related_decisions   = ["DEC-0005", "DEC-0006"]
artifacts           = ["ether-chat-backend-ports/src/main/java/dev/rafex/chat/chat/port/AiGateway.java",
                       "ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/",
                       "ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/chat/handler/",
                       "ether-chat-backend-bootstrap/src/main/java/dev/rafex/chat/bootstrap/AppBootstrap.java",
                       "openapi/openapi.yaml"]
validation          = ["mvn -pl ether-chat-backend-transport-jetty -am test",
                       "POST /api/chat/message responde con proveedor real cuando DEEPSEEK_API_KEY existe",
                       "WS /ws/chat responde con JWT valido"]
+++

# SPEC-0004: ai-realtime-mvp

## Resumen

Incorporar `ether-ai-deepseek` como implementacion real del puerto
`AiGateway` y habilitar transporte realtime MVP sobre `ether-websocket`
en `/ws/chat` sin romper los endpoints REST existentes.

## Problema

Hoy el backend responde chat con `EchoAiGateway`, lo que bloquea pruebas
reales con modelo externo y no cubre la via WebSocket requerida para UX
realtime.

## Objetivo

El backend soporta dos caminos funcionales para chat:

1. REST `POST /api/chat/message` con proveedor DeepSeek real cuando hay
   configuracion valida.
2. WebSocket `/ws/chat` autenticado con JWT que entrega respuestas del
   agente para el mismo flujo de conversacion.

## Alcance

**Incluye:**

- Config de proveedor:
  - `DEEPSEEK_API_KEY` (requerido para provider real)
  - `DEEPSEEK_BASE_URL` (opcional)
  - `DEEPSEEK_MODEL` (default definido por spec)
  - `AI_PROVIDER` (`echo|deepseek`, default `echo`)
- Implementacion `DeepseekAiGateway` usando `ether-ai-deepseek`.
- Seleccion de provider en `AppBootstrap` via config.
- `WebSocketChatHandler` sobre `ether-websocket-jetty12` en `/ws/chat`.
- Verificacion JWT al handshake o primer mensaje (segun API de Ether WS).
- Contrato de mensajes JSON MVP:
  - entrada: `{ "token":"...", "message":"...", "conversation_id":"..." }`
  - salida: `{ "content":"...", "conversation_id":"..." }`
- Reuso de `ChatService` para evitar duplicar logica de negocio.
- Tests unitarios de adaptador DeepSeek (sin red real) y handler WS.

**Excluye:**

- Streaming token-by-token desde proveedor.
- Multiplexado de sesiones WS en una sola conexion.
- Herramientas/function-calling/RAG.
- Cambios de contrato REST existentes.

## Requisitos funcionales

- RF-1: Si `AI_PROVIDER=deepseek` y `DEEPSEEK_API_KEY` existe, `POST /api/chat/message`
  usa proveedor real y no `EchoAiGateway`.
- RF-2: Si `AI_PROVIDER` no es `deepseek` o falta config critica, el
  sistema usa fallback seguro `EchoAiGateway`.
- RF-3: `WS /ws/chat` con JWT valido y `message` responde payload JSON
  con `content` y `conversation_id`.
- RF-4: `WS /ws/chat` sin JWT o JWT invalido rechaza flujo con error
  equivalente a `401`.
- RF-5: Reusar `conversation_id` en WS mantiene continuidad.

## Requisitos no funcionales

- RNF-1: Ningun secreto se loggea en texto plano.
- RNF-2: El handler WS no contiene logica de negocio, solo traduccion
  transporte ↔ caso de uso.
- RNF-3: La app sigue compilando en Java 21 y mantiene arquitectura
  hexagonal.

## Criterios de aceptacion

- Dado `AI_PROVIDER=deepseek` y credenciales validas,
  cuando `POST /api/chat/message`,
  entonces la respuesta proviene de DeepSeek.

- Dado `AI_PROVIDER=echo`,
  cuando `POST /api/chat/message`,
  entonces la respuesta conserva prefijo `"[ECHO] "`.

- Dado un cliente WS con JWT valido,
  cuando envia mensaje a `/ws/chat`,
  entonces recibe respuesta JSON con `content` y `conversation_id`.

- Dado un cliente WS sin JWT,
  cuando intenta usar `/ws/chat`,
  entonces la sesion se rechaza.

## Dependencias

- SPEC-0001, SPEC-0002, SPEC-0003 en `done`.

## Riesgos

- Cambios en APIs de `ether-ai-deepseek` o `ether-websocket-jetty12`
  pueden requerir ajustes de contrato.
- Si el proveedor remoto falla, se debe mantener degradacion controlada.
