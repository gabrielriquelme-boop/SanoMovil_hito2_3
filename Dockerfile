# =====================================================================
# Empaqueta el servicio de Sano Móvil. Sirve para:
#   - levantar todo con un solo comando (docker compose up --build), o
#   - desplegarlo en la nube (Render, etc.).
# Se construye desde la RAÍZ del repositorio.
# =====================================================================

# Etapa 1: compilar con el Gradle wrapper del proyecto
FROM eclipse-temurin:21-jdk AS compilar
WORKDIR /app
COPY backend/ ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon installDist

# Etapa 2: imagen final, más liviana (solo Java para ejecutar)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilar /app/build/install/sano-movil-backend/ ./
# Memoria acotada: los planes gratuitos de la nube tienen solo 512 MB de RAM
ENV JAVA_OPTS="-Xmx320m -XX:+UseSerialGC"
EXPOSE 8080
CMD ["bin/sano-movil-backend"]
