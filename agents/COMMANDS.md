# COMMANDS.md

Comandos operativos del proyecto. Ejecutar siempre desde la raiz del
repo a menos que se indique lo contrario.

## Build

```bash
# Compilar todos los modulos
./mvnw clean compile

# Compilar y empaquetar (fat-jar en transport-jetty)
./mvnw clean package -DskipTests

# Compilar con tests
./mvnw clean verify
```

## Tests

```bash
# Todos los tests
./mvnw test

# Solo un modulo
./mvnw test -pl ether-chat-backend-core

# Solo una clase de test
./mvnw test -pl ether-chat-backend-core -Dtest=AuthServiceImplTest

# Tests de arquitectura
./mvnw test -pl ether-chat-backend-architecture-tests
```

## Run

```bash
# Con fat-jar (despues de package)
java -jar ether-chat-backend-transport-jetty/target/ether-chat-backend-transport-jetty-jar-with-dependencies.jar

# Con variables de entorno
SERVER_PORT=8080 \
AUTH_DB_PATH=./data/auth.db \
CHAT_DB_PATH=./data/chat.db \
JWT_SECRET=supersecret-minimum-32-characters! \
java -jar ether-chat-backend-transport-jetty/target/ether-chat-backend-transport-jetty-jar-with-dependencies.jar

# Con Maven exec (sin empaquetar, desde transport-jetty)
cd ether-chat-backend-transport-jetty
./mvnw exec:java -Dexec.mainClass=dev.rafex.chat.App

# Con Glowroot APM
java -javaagent:/path/to/glowroot/glowroot.jar \
  -jar ether-chat-backend-transport-jetty/target/ether-chat-backend-transport-jetty-jar-with-dependencies.jar
```

## Verificacion de API (curl)

```bash
# Health check
curl http://localhost:8080/health

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"password123"}'

# Mensaje de chat (sustituir TOKEN por el retornado en login)
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"message":"Hola","conversation_id":null}'
```

## Maven Wrapper — instalar en un modulo nuevo

```bash
# Desde el directorio del modulo nuevo
mvn wrapper:wrapper -Dmaven=3.9.9
# O copiar .mvn/ + mvnw + mvnw.cmd desde el root
```

## Limpieza

```bash
./mvnw clean

# Limpiar bases de datos de desarrollo
rm -f data/auth.db data/chat.db
```

## Utilidades

```bash
# Ver dependencias de un modulo
./mvnw dependency:tree -pl ether-chat-backend-transport-jetty

# Verificar que no hay dependencias ciclicas ni violaciones de capas
./mvnw test -pl ether-chat-backend-architecture-tests

# Generar sitio con reportes
./mvnw site
```
