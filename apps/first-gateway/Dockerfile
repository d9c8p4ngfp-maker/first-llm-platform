# ---- Build ----
FROM docker.m.daocloud.io/library/eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache maven

# Layer 1: download dependencies (cached unless pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -Dmaven.test.skip=true

# Layer 2: copy src and compile (cached unless src changes)
COPY src ./src
RUN mvn package -Dmaven.test.skip=true -q

# ---- Runtime ----
FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget && addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/gateway-*.jar app.jar
RUN mkdir -p /app/uploads && chown -R app:app /app
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
