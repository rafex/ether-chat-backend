# SPEC.md

Punto de entrada al trabajo activo del proyecto.

## Estado general

Este archivo enruta a las specs por iniciativa. El trabajo esta dividido
en tres iniciativas secuenciales. Cada una tiene su spec y sus tareas.

## Specs activas

| ID | Iniciativa | Estado | Carpeta |
|----|-----------|--------|---------|
| SPEC-0001 | backend-bootstrap | `active` | `specs/backend-bootstrap/SPEC.md` |
| SPEC-0002 | auth-service | `draft` | `specs/auth-service/SPEC.md` |
| SPEC-0003 | chat-service | `draft` | `specs/chat-service/SPEC.md` |

## Orden de ejecucion

```
SPEC-0001  ──►  SPEC-0002  ──►  SPEC-0003
(estructura)    (auth REST)     (chat REST)
```

SPEC-0002 y SPEC-0003 dependen del proyecto Maven generado en SPEC-0001.

## Como empezar

1. Leer `agents/ARCHITECTURE.md` para entender la estructura de modulos.
2. Leer `agents/STACK.md` para las coordenadas Maven exactas.
3. Leer `agents/DECISIONS.md` para restricciones de diseno.
4. Abrir `specs/backend-bootstrap/SPEC.md` y ejecutar las tareas en
   `tasks/backend-bootstrap/TASKS.md`.
