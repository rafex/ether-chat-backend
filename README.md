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

## Inicio rapido

```bash
# Compilar todo desde la raiz
./mvnw clean package -DskipTests

# Ejecutar
java -jar ether-chat-backend-transport-jetty/target/ether-chat-backend-transport-jetty-jar-with-dependencies.jar
```

## Puerto por defecto

`8080` — configurable con `SERVER_PORT`.
