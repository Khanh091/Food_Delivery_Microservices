FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=builder /workspace/target/payment-service-0.0.1-SNAPSHOT.jar app.jar
USER spring
EXPOSE 8109
ENTRYPOINT ["java", "-jar", "app.jar"]
