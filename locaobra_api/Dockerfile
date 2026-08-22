# ------------------------------------------------------------------
# Estágio 1 — BUILD: compila e empacota o jar com Maven
# ------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copia o pom primeiro para aproveitar o cache de dependências
COPY pom.xml .
RUN mvn -B dependency:go-offline -q || true

# Copia o restante do código e compila
COPY src ./src
RUN mvn -B clean package -DskipTests

# ------------------------------------------------------------------
# Estágio 2 — RUNTIME: imagem enxuta só com a JRE
# ------------------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

EXPOSE ${PORT:-8080}

# Copia o jar gerado no estágio de build
COPY --from=build /build/target/*.jar app.jar

# Cria usuário não-root (boa prática de segurança) e só então cria a pasta
# de uploads, já com esse usuário como dono — senão ela fica de root e o
# appuser não consegue escrever nela (mkdirs() falha silenciosamente e o
# upload quebra com FileNotFoundException na hora de gravar o arquivo).
RUN useradd --create-home appuser \
    && mkdir -p /app/uploads \
    && chown -R appuser:appuser /app
USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]