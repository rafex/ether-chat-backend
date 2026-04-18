# PRODUCT.md

Fuente de verdad del producto.

## Problema

Los frontends de chat con agentes IA necesitan un backend que gestione
autenticacion, conversaciones y enrutamiento de mensajes hacia un modelo
de lenguaje. Sin un backend propio, los frontends quedan acoplados a
mocks o a SDKs de proveedores que exponen credenciales en el cliente.

## Usuarios

- **Frontend ether-chat-web**: consume los endpoints REST y WebSocket
  para autenticar usuarios y enviar mensajes al agente.
- **Integradores**: equipos que despliegan el backend apuntando su propio
  proveedor de IA (OpenAI, Anthropic, Ollama, modelo propio).
- **Desarrolladores**: que usan el modo sandbox local con SQLite sin
  infraestructura externa.

## Objetivos

- Proveer los endpoints minimos que `ether-chat-web` espera:
  `POST /api/auth/login`, `POST /api/chat/message`, `GET /health`.
- Gestionar sesiones JWT sin framework de seguridad externo.
- Persistir conversaciones y mensajes en SQLite con WAL.
- Ser sustituible: el adaptador de IA (AiGateway) es un puerto que
  cualquier implementacion puede cubrir.

## Metricas de exito

- El frontend puede autenticarse y enviar mensajes contra este backend
  sin cambios en su codigo.
- Un integrador puede apuntar un proveedor de IA diferente implementando
  solo la interfaz `AiGateway`.
- El build completo pasa sin errores: `./mvnw clean verify`.

## No objetivos

- No es un proveedor de IA: no contiene modelos de lenguaje.
- No es un sistema multitenancy: una instancia sirve a un tenant.
- No incluye administracion de usuarios (registro, recuperacion de
  contrasena, roles).
- No gestiona archivos ni media.

## Valor diferencial

Backend minimo, portable y sin magic. Cada capa es explicita: el DI es
manual via `ether-di`, la DB es SQLite con JDBC plano via `ether-jdbc`,
el servidor es Jetty 12 via `ether-http-jetty12`. Un integrador puede
leer el codigo y entender exactamente que hace sin necesidad de conocer
un framework de terceros.
