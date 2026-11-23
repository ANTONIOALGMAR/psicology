FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r appuser && useradd -r -g appuser appuser \
    && mkdir -p /app/uploads
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
ENTRYPOINT ["sh","-c","mkdir -p /app/uploads && chown -R appuser:appuser /app/uploads && exec su -s /bin/sh appuser -c \"export PATH='$PATH'; exec java $JAVA_OPTS -jar /app/app.jar\""]
