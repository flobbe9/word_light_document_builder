# FROM gradle:jdk17-alpine
FROM eclipse-temurin:17-jdk-alpine-3.22

WORKDIR /app

COPY ./src ./src
COPY ./gradle ./gradle 
COPY ./build.gradle \
     ./settings.gradle \
     ./gradlew \
     ./.env \
     ./

# make gradle wrapper executable
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

RUN apk update;
RUN apk add libreoffice;

ENTRYPOINT ./gradlew bootRun