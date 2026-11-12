package co.eci.c15.chat.infrastructure.estado;

import co.eci.c15.chat.domain.MensajeChat;
import co.eci.c15.chat.infrastructure.persistence.ChatEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatRedisTest extends RedisDePrueba {

    private static final Instant AHORA = Instant.parse("2026-11-03T15:00:00Z");

    private ChatRedis chat;

    @BeforeEach
    void setUp() {
        chat = new ChatRedis(redis, JSON, new CaducidadChat(Duration.ofMinutes(10)));
    }

    private static MensajeChat mensaje(String canal, String texto) {
        return MensajeChat.crear(canal, "ana", texto, AHORA);
    }

    private long segundosDeVida(String canal) {
        return redis.getExpire(ChatRedis.PREFIJO + canal, TimeUnit.SECONDS);
    }

    @Test
    void guardaYDevuelveLosMensajesEnOrdenConTodosSusDatos() {
        MensajeChat primero = mensaje("global", "hola");
        chat.guardar(primero);
        chat.guardar(mensaje("global", "chao"));

        assertEquals(primero, chat.historial("global").get(0));
        assertEquals("chao", chat.historial("global").get(1).texto());
    }

    @Test
    void soloGuardaLosUltimosMensajes() {
        for (int i = 0; i < ChatEnMemoria.MAX_MENSAJES + 5; i++) chat.guardar(mensaje("global", "m" + i));

        var historial = chat.historial("global");
        assertEquals(ChatEnMemoria.MAX_MENSAJES, historial.size());
        assertEquals("m5", historial.get(0).texto());
    }

    @Test
    void cadaCanalCaducaSegunSuTipo() {
        chat.guardar(mensaje("global", "a"));
        chat.guardar(mensaje("salas/s1", "b"));
        chat.guardar(mensaje("partidas/m1/equipos/e1", "c"));

        assertTrue(segundosDeVida("global") > 3500 && segundosDeVida("global") <= 3600);
        assertTrue(segundosDeVida("salas/s1") > 1700 && segundosDeVida("salas/s1") <= 1800);
        // 5 min de partida + 10 de margen
        assertTrue(segundosDeVida("partidas/m1/equipos/e1") > 1450 && segundosDeVida("partidas/m1/equipos/e1") <= 1500);
    }

    @Test
    void borrarDejaElCanalVacioYUnCanalSinMensajesEstaVacio() {
        chat.guardar(mensaje("salas/s1", "a"));

        chat.borrar("salas/s1");

        assertTrue(chat.historial("salas/s1").isEmpty());
        assertTrue(chat.historial("nunca-uso").isEmpty());
    }
}
