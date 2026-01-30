FROM eclipse-temurin:21.0.9_10-jdk-alpine-3.23

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
RUN ./gradlew clean build


FROM eclipse-temurin:21.0.9_10-jre-alpine-3.23

WORKDIR /app

ENV JAR_FILE_NAME='document_builder-SNAPSHOT-0.0.1.jar'
COPY --from=0 /app/build/libs/${JAR_FILE_NAME} ./${JAR_FILE_NAME}
COPY --from=0 /app/.env ./.env

# make pdf conversion work 
RUN apk update;
RUN apk add libreoffice;

ENTRYPOINT java -jar ${JAR_FILE_NAME}