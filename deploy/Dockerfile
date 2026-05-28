FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY . .
RUN ./mvnw clean package -DskipTests -pl '!frontend' -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/backend/app/target/app-main.jar /app/app.jar
COPY --from=build /build/deploy/commons-prod.properties /app/commons-prod.properties
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod", "--spring.config.additional-location=file:/app/commons-prod.properties"]
