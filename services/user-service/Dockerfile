FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw \
    && ./mvnw dependency:go-offline -B

COPY src src

RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=builder /workspace/target/*.jar /app/app.jar

USER spring:spring

EXPOSE 8101

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
