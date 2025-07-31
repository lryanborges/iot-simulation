# Usando OpenJDK 21 da Eclipse Temurin
FROM eclipse-temurin:21-jdk

# Definindo o diretório de trabalho no contêiner
WORKDIR /app

# Copiando o JAR da aplicação para o contêiner
COPY target/sistmonitoramento-0.0.1-SNAPSHOT.jar app.jar

# Expondo a porta que o Spring Boot usa
EXPOSE 8080

# Comando para rodar a aplicação
CMD ["java", "-jar", "app.jar"]
