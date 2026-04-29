FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=ether-chat-backend-transport-jetty/target/ether-chat-backend-transport-jetty-jar-with-dependencies.jar
COPY ${JAR_FILE} /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
