# ============================================================
# Stage 1 — Build (Maven + Java 21)
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar el Maven wrapper primero (capa cacheada si pom.xml no cambió)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Convertir CRLF → LF (necesario cuando el proyecto viene de Windows)
RUN sed -i 's/\r//' mvnw && chmod +x mvnw

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src src

# Compilar el JAR (sin tests — el corrector los corre por separado)
RUN ./mvnw package -DskipTests -B


# ============================================================
# Stage 2 — Runtime (solo JRE, imagen más liviana)
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el JAR generado en la etapa anterior
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Puerto que expone Spring Boot
EXPOSE 8080

# Arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
