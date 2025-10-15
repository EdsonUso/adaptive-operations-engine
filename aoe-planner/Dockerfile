# Dockerfile for aoe-planner

# --- Build Stage ---
# Usa uma imagem Maven com OpenJDK 17 para compilar o projeto
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copia o pom.xml e baixa as dependências para acelerar builds futuros
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o resto do código fonte e compila o projeto
COPY src ./src
RUN mvn package -DskipTests

# --- Final Stage ---
# Usa uma imagem JRE (Java Runtime Environment) slim para a execução
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia o arquivo .jar compilado do estágio de build para a imagem final
COPY --from=builder /app/target/*.jar app.jar

# Expõe a porta 8080, que é a padrão para aplicações Spring Boot
EXPOSE 8080

# Define o comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
