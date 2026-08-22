FROM eclipse-temurin:25-jdk
EXPOSE 5013

RUN mkdir -p /usr/app
WORKDIR /usr/app

COPY build/libs/ledger-svc-0.0.1-SNAPSHOT.jar /usr/app/

# --enable-native-access: tigerbeetle-java loads its native client via System.load (JEP 472)
ENV JAVA_OPTS="-Xmx256m --enable-native-access=ALL-UNNAMED"

CMD exec java $JAVA_OPTS -jar ledger-svc-0.0.1-SNAPSHOT.jar
