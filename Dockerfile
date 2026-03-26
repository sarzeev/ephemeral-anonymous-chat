FROM eclipse-temurin:21-jdk

WORKDIR /app

ENV DEBIAN_FRONTEND=noninteractive
ENV PORT=8080

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests -B

RUN useradd --system --create-home spring
USER spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar target/ephemeral-anonymous-chat-backend-0.0.1-SNAPSHOT.jar"]
