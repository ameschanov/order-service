FROM openjdk:18-alpine

COPY target/order-service.jar /app.jar

CMD ["java","-jar","-Dfile.encoding=UTF-8","-Dsun.jnu.encoding=UTF-8","-XX:+UseG1GC","-XX:+UseCompressedOops","-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1","-Duser.timezone=Europe/Moscow","/app.jar"]
