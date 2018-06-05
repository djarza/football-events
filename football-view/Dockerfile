FROM openjdk:8u162-jre-slim-stretch
VOLUME /tmp
COPY target/*SNAPSHOT.jar /app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8083
