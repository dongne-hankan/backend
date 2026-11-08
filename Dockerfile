FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
# Render 무료 플랜(512MB)에 맞춰 메모리를 제한하고, 날짜 계산은 한국 시간을 쓴다.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Duser.timezone=Asia/Seoul"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
