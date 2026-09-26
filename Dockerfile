FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -Dmaven.test.skip=true

EXPOSE 10000

CMD ["sh", "-c", "java -jar target/*.jar --server.port=${PORT:-10000}"]
