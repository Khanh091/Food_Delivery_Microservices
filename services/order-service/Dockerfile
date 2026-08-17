FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw

COPY src src

RUN ./mvnw -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=builder /workspace/target/order-service-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

EXPOSE 8106

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
