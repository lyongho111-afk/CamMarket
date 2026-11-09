# Build the executable jar with the same Gradle and Java versions as the project.
FROM gradle:9.3.0-jdk25 AS build
WORKDIR /workspace
COPY --chown=gradle:gradle . .
RUN gradle --no-daemon bootJar

# Keep the runtime image to a JRE and run the application as an unprivileged user.
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring && mkdir -p /data/uploads && chown -R spring:spring /data
ENV CAMPUS_UPLOAD_DIR=/data/uploads
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:MaxRAMPercentage=55.0 -XX:InitialRAMPercentage=15.0 -Duser.timezone=Asia/Seoul"
EXPOSE 8080
COPY --from=build --chown=spring:spring /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar
USER spring
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
