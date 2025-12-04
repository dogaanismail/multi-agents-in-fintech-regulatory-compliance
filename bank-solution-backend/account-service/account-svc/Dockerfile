FROM eclipse-temurin:25-jdk
EXPOSE 5002

RUN mkdir -p /usr/app
WORKDIR /usr/app

COPY build/libs/account-svc-0.0.1-SNAPSHOT.jar /usr/app/

ENV JAVA_OPTS="-Xmx256m"

CMD exec java $JAVA_OPTS -jar account-svc-0.0.1-SNAPSHOT.jar
