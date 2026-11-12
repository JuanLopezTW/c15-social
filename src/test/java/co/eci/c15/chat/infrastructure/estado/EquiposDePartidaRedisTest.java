package co.eci.c15.chat.infrastructure.estado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EquiposDePartidaRedisTest extends RedisDePrueba {

    private static final Map<String, List<String>> EQUIPOS = Map.of("e1", List.of("ana", "beto"), "e2", List.of("caro"));

    private EquiposDePartidaRedis equipos;

    @BeforeEach
    void setUp() {
        equipos = new EquiposDePartidaRedis(redis, JSON, new CaducidadChat(Duration.ofMinutes(10)));
    }

    @Test
    void guardaLosEquiposConLaCaducidadDeLaPartida() {
        equipos.guardar("m1", EQUIPOS);

        assertEquals(EQUIPOS, equipos.de("m1").orElseThrow());
        long segundos = redis.getExpire(EquiposDePartidaRedis.PREFIJO + "m1", TimeUnit.SECONDS);
        assertTrue(segundos > 1450 && segundos <= 1500, "caduca en " + segundos + " s");
    }

    @Test
    void unaPartidaDesconocidaOBorradaNoTieneEquipos() {
        equipos.guardar("m1", EQUIPOS);

        equipos.borrar("m1");

        assertTrue(equipos.de("m1").isEmpty());
        assertTrue(equipos.de("nunca").isEmpty());
    }
}
