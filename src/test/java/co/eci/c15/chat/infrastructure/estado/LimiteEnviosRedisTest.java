package co.eci.c15.chat.infrastructure.estado;

import co.eci.c15.chat.application.LimiteEnvios;
import co.eci.c15.chat.domain.DemasiadosMensajesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LimiteEnviosRedisTest extends RedisDePrueba {

    private static final Instant AHORA = Instant.parse("2026-11-03T15:00:00Z");

    private LimiteEnviosRedis limite;

    @BeforeEach
    void setUp() {
        limite = new LimiteEnviosRedis(redis);
    }

    @Test
    void dejaElMaximoYFrenaElSiguienteHastaQuePaseLaVentana() {
        for (int i = 0; i < LimiteEnvios.MAX_MENSAJES; i++) limite.registrar("ana", AHORA.plusMillis(i));

        assertThrows(DemasiadosMensajesException.class, () -> limite.registrar("ana", AHORA.plusMillis(10)));
        assertDoesNotThrow(() -> limite.registrar("beto", AHORA));
        assertDoesNotThrow(() -> limite.registrar("ana", AHORA.plus(LimiteEnvios.VENTANA).plusMillis(1)));
    }

    @Test
    void laClaveCaducaConLaVentana() {
        limite.registrar("ana", AHORA);

        long ms = redis.getExpire(LimiteEnviosRedis.PREFIJO + "ana", TimeUnit.MILLISECONDS);
        assertTrue(ms > 0 && ms <= LimiteEnvios.VENTANA.toMillis(), "caduca en " + ms + " ms");
    }

    @Test
    void variosEnviosALaVezNoSePasanDelMaximo() throws Exception {
        int intentos = 20;
        ExecutorService hilos = Executors.newFixedThreadPool(intentos);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < intentos; i++) {
            resultados.add(hilos.submit(() -> {
                largada.await();
                return intentar();
            }));
        }
        largada.countDown();
        int aceptados = 0;
        for (Future<Boolean> r : resultados) if (r.get(10, TimeUnit.SECONDS)) aceptados++;
        hilos.shutdown();

        assertEquals(LimiteEnvios.MAX_MENSAJES, aceptados);
    }

    private boolean intentar() {
        try {
            limite.registrar("ana", AHORA);
            return true;
        } catch (DemasiadosMensajesException e) {
            return false;
        }
    }
}
