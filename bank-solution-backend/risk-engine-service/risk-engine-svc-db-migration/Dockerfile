FROM eclipse-temurin:25-jdk

RUN mkdir -p /usr/app
WORKDIR /usr/app

COPY build/libs/risk-engine-svc-db-migration-0.0.1-SNAPSHOT.jar /usr/app/

ENV JAVA_OPTS="-Xmx64m"

CMD exec java $JAVA_OPTS -jar risk-engine-svc-db-migration-0.0.1-SNAPSHOT.jar
