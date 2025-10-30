FROM openjdk:17-jdk
WORKDIR /app
COPY target/qmodel-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
EXPOSE 4123
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:4123", "-jar", "app.jar"]