# syntax=docker/dockerfile:1
# ======================================================================================
# Imagen multi-etapa: compilacion con Maven, runtime solo con JRE y usuario sin permisos.
# ======================================================================================

# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Capa de dependencias: se reaprovecha mientras el pom no cambie
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario dedicado sin privilegios (la imagen alpine no incluye adduser de shadow)
RUN addgroup -S spring && adduser -S -G spring spring

WORKDIR /app
COPY --from=build --chown=spring:spring /build/target/medirecord.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
-XX:+ExitOnOutOfMemoryError \
-XX:+UseParallelGC \
-Djava.security.egd=file:/dev/./urandom \
-Dfile.encoding=UTF-8 \
-Duser.timezone=UTC"

EXPOSE 8080

# Healthcheck del contenedor: /actuator/health (permitido sin token)
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
	CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]