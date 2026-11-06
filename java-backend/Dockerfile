FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Xms64m -Xmx512m"
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app \
    && mkdir -p /app /data/uploads && chown -R app:app /app /data/uploads
WORKDIR /app
COPY --from=build --chown=app:app /app/target/admin-template-backend-0.1.0-SNAPSHOT.jar app.jar
USER app
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:3000/ > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
