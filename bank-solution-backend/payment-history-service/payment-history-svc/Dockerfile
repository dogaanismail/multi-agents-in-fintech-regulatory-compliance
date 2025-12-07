FROM eclipse-temurin:25-jdk
EXPOSE 5005

RUN mkdir -p /usr/app
WORKDIR /usr/app

COPY build/libs/payment-history-svc-0.0.1-SNAPSHOT.jar /usr/app/

ENV JAVA_OPTS="-Xmx256m"

CMD exec java $JAVA_OPTS -jar payment-history-svc-0.0.1-SNAPSHOT.jar
