FROM amazoncorretto:21-alpine

WORKDIR /app

COPY build/libs/TGTripBot-0.0.1-SNAPSHOT-all.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"] 