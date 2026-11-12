package co.eci.c15.chat.infrastructure.estado;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Un Redis real en Docker para probar los adaptadores; sin Docker estas pruebas se saltan. */
@Testcontainers(disabledWithoutDocker = true)
abstract class RedisDePrueba {

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.8.2-alpine").withExposedPorts(6379);

    static LettuceConnectionFactory conexion;
    static StringRedisTemplate redis;
    static final ObjectMapper JSON = JsonMapper.builder().findAndAddModules().build();

    @BeforeAll
    static void arrancarRedis() {
        if (!REDIS.isRunning()) REDIS.start();
        conexion = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        conexion.afterPropertiesSet();
        conexion.start();
        redis = new StringRedisTemplate(conexion);
    }

    @AfterAll
    static void cerrarConexion() {
        conexion.destroy();
    }

    @BeforeEach
    void limpiar() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }
}
