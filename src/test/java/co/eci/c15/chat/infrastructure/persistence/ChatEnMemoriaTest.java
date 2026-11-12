package co.eci.c15.chat.infrastructure.persistence;

import co.eci.c15.chat.domain.MensajeChat;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatEnMemoriaTest {

    private final ChatEnMemoria repo = new ChatEnMemoria();

    private static MensajeChat mensaje(String salaId, String texto) {
        return MensajeChat.crear(salaId, "ana", texto, Instant.EPOCH);
    }

    @Test
    void guardaYDevuelveEnOrden() {
        repo.guardar(mensaje("s1", "uno"));
        repo.guardar(mensaje("s1", "dos"));

        assertEquals(List.of("uno", "dos"), repo.historial("s1").stream().map(MensajeChat::texto).toList());
    }

    @Test
    void unaSalaSinMensajesDevuelveListaVacia() {
        assertTrue(repo.historial("nada").isEmpty());
    }

    @Test
    void soloConservaLosUltimosMensajes() {
        for (int i = 0; i < ChatEnMemoria.MAX_MENSAJES + 5; i++) repo.guardar(mensaje("s1", "m" + i));

        List<MensajeChat> historial = repo.historial("s1");
        assertEquals(ChatEnMemoria.MAX_MENSAJES, historial.size());
        assertEquals("m5", historial.get(0).texto());
        assertEquals("m" + (ChatEnMemoria.MAX_MENSAJES + 4), historial.get(historial.size() - 1).texto());
    }

    @Test
    void borrarSoloAfectaASuSala() {
        repo.guardar(mensaje("s1", "uno"));
        repo.guardar(mensaje("s2", "otro"));

        repo.borrar("s1");

        assertTrue(repo.historial("s1").isEmpty());
        assertEquals(1, repo.historial("s2").size());
    }

    @Test
    void elHistorialEsUnaCopiaQueNoCambiaConMensajesNuevos() {
        repo.guardar(mensaje("s1", "uno"));
        List<MensajeChat> antes = repo.historial("s1");

        repo.guardar(mensaje("s1", "dos"));

        assertEquals(1, antes.size());
    }

    @Test
    void mensajesSimultaneosNoSePierden() throws Exception {
        int hilos = 8;
        int porHilo = 5;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            for (int h = 0; h < hilos; h++) {
                pool.submit(() -> {
                    salida.await();
                    for (int i = 0; i < porHilo; i++) repo.guardar(mensaje("s1", "x"));
                    return null;
                });
            }
            salida.countDown();
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(hilos * porHilo, repo.historial("s1").size());
    }
}
