FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/SchoolApplicationDataJPA-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
