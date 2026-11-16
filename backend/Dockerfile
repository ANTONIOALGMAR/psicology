FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn 2>/dev/null || true
COPY mvnw pom.xml ./
COPY src ./src
RUN if [ -f mvnw ]; then ./mvnw -q -DskipTests package; else apk add --no-cache maven && mvn -q -DskipTests package; fi

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
