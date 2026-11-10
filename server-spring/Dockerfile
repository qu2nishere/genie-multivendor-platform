# Multi-stage: build Maven → JRE 21 slim, user không phải root (TECH_STACK mục 2.3)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
ARG VERSION=0.0.1-SNAPSHOT
RUN ./mvnw -B -q package -DskipTests -Drevision=${VERSION#v} \
    && cp target/genie-backend-*.jar /app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S genie && adduser -S genie -G genie
WORKDIR /app
COPY --from=build /app.jar app.jar
USER genie
EXPOSE 8080
ENV JAVA_OPTS="-Xms512m -Xmx1536m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
