# SPEC.md

Punto de entrada al trabajo activo del proyecto.

## Estado general

La base del backend (SPEC-0001 a SPEC-0003) esta cerrada y hay una
iniciativa activa para proveedor real + transporte realtime MVP.

## Specs registradas

| ID | Iniciativa | Estado | Carpeta |
|----|-----------|--------|---------|
| SPEC-0001 | backend-bootstrap | `done` | `specs/backend-bootstrap/SPEC.md` |
| SPEC-0002 | auth-service | `done` | `specs/auth-service/SPEC.md` |
| SPEC-0003 | chat-service | `done` | `specs/chat-service/SPEC.md` |
| SPEC-0004 | ai-realtime-mvp | `active` | `specs/ai-realtime-mvp/SPEC.md` |

## Secuencia completada

```
SPEC-0001  ──►  SPEC-0002  ──►  SPEC-0003
(estructura)    (auth REST)     (chat REST)
```

La base multi-modulo, el login y el endpoint de chat quedaron cerrados
en ese orden.

## Trabajo actual

```
SPEC-0004
(deepseek + websocket mvp)
```

## Como empezar

1. Leer `agents/ARCHITECTURE.md` para entender la estructura actual.
2. Ejecutar `tasks/ai-realtime-mvp/TASKS.md` en orden de dependencias.
3. Actualizar `agents/TRACEABILITY.md` al completar la spec.
