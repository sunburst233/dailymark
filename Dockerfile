FROM node:26-alpine AS frontend
WORKDIR /app/frontend
ARG DAILYMARK_BASE_PATH=
ENV VITE_BASE_PATH=${DAILYMARK_BASE_PATH}
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
ARG DAILYMARK_BASE_PATH=
ARG SERVER_PORT=8080
RUN useradd -r -u 1001 dailymark && mkdir -p /data && chown -R dailymark /data
COPY --from=backend /app/backend/target/*.jar app.jar
USER dailymark
VOLUME ["/data"]
EXPOSE ${SERVER_PORT}
ENV JAVA_OPTS="-Xms128m -Xmx512m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
