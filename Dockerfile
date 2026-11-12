# --- Etapa 1: compilar y generar el jar ejecutable ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
# Las dependencias se bajan en su propia capa: si solo cambia el codigo, no se vuelven a bajar
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests -Djacoco.skip=true package
# Separa el jar en capas (dependencias aparte del codigo): si solo cambia el codigo, solo cambia esa capa
RUN cp target/c15-social-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination capas

# --- Etapa 2: imagen liviana solo con el JRE y la app ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/capas/dependencies/ ./
COPY --from=build /src/capas/spring-boot-loader/ ./
COPY --from=build /src/capas/snapshot-dependencies/ ./
COPY --from=build /src/capas/application/ ./
# Arranque de entrenamiento para el cache de clases (CDS): con el la app arranca mas rapido.
# Se detiene apenas se crean los beans, antes de abrir puertos o conectarse a algo. El relay a Rabbit
# y la creacion de topics de Kafka van apagados, y la base no se toca (sin ddl ni metadatos).
# No hay claves: el build no tiene el .env.
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh \
        -DDB_USERNAME= -DDB_PASSWORD= -DSTOMP_RELAY=false -DKAFKA_CREAR_TOPICS=false \
        -Dspring.jpa.hibernate.ddl-auto=none \
        -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
        -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
        -jar app.jar
# Corre sin privilegios de root: si alguien explotara una falla, no tendria root en el contenedor
RUN useradd --system --no-create-home --uid 10001 c15
USER 10001
# Commit de esta imagen: lo expone GET /api/version
ARG GIT_SHA=dev
ENV APP_VERSION=$GIT_SHA
EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-jar", "app.jar"]
