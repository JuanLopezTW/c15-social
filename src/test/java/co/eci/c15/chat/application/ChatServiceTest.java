package co.eci.c15.chat.application;

import co.eci.c15.chat.domain.ChatNoDisponibleException;
import co.eci.c15.chat.domain.DemasiadosMensajesException;
import co.eci.c15.chat.domain.JugadorFueraDeSalaException;
import co.eci.c15.chat.domain.MensajeChat;
import co.eci.c15.chat.infrastructure.estado.EquiposDePartidaEnMemoria;
import co.eci.c15.chat.infrastructure.estado.LimiteEnviosEnMemoria;
import co.eci.c15.chat.infrastructure.persistence.ChatEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-13T15:00:00Z");
    private static final Map<String, List<String>> EQUIPOS = Map.of("e1", List.of("ana", "beto"), "e2", List.of("caro"));

    private SalasFalsas salas;
    private ChatEnMemoria mensajes;
    private ChatNotifier notifier;
    private RelojMovible reloj;
    private ChatService chat;

    @BeforeEach
    void setUp() {
        salas = new SalasFalsas();
        salas.enEspera("s1", "ana", "beto");
        mensajes = new ChatEnMemoria();
        notifier = mock(ChatNotifier.class);
        reloj = new RelojMovible(AHORA);
        chat = new ChatService(mensajes, salas, Set.of("ana", "beto", "caro")::contains, notifier,
                new LimiteEnviosEnMemoria(), new EquiposDePartidaEnMemoria(), reloj);
    }

    private List<String> textos(List<MensajeChatDto> lista) {
        return lista.stream().map(MensajeChatDto::texto).toList();
    }

    // Sala de espera

    @Test
    void unJugadorDeLaSalaEnviaYSeAvisaATodos() {
        MensajeChatDto dto = chat.enviarSala("s1", "ana", " hola ");

        assertEquals("salas/s1", dto.canal());
        assertEquals("ana", dto.userId());
        assertEquals("hola", dto.texto());
        assertEquals(AHORA, dto.enviadoEn());
        verify(notifier).mensajeNuevo(dto);
        assertEquals(List.of(dto), chat.historialSala("s1", "beto"));
    }

    @Test
    void elHistorialVieneEnOrdenDeEnvio() {
        chat.enviarSala("s1", "ana", "uno");
        chat.enviarSala("s1", "beto", "dos");
        chat.enviarSala("s1", "ana", "tres");

        assertEquals(List.of("uno", "dos", "tres"), textos(chat.historialSala("s1", "ana")));
    }

    @Test
    void elHistorialDeUnaSalaSinMensajesEstaVacio() {
        assertTrue(chat.historialSala("s1", "ana").isEmpty());
    }

    @Test
    void cadaSalaTieneSuPropioChat() {
        salas.enEspera("s2", "caro");
        chat.enviarSala("s1", "ana", "en s1");
        chat.enviarSala("s2", "caro", "en s2");

        assertEquals(List.of("en s1"), textos(chat.historialSala("s1", "ana")));
        assertEquals(List.of("en s2"), textos(chat.historialSala("s2", "caro")));
    }

    @Test
    void quienNoEstaEnLaSalaNoPuedeEscribirNiLeer() {
        assertThrows(JugadorFueraDeSalaException.class, () -> chat.enviarSala("s1", "intruso", "hola"));
        assertThrows(JugadorFueraDeSalaException.class, () -> chat.historialSala("s1", "intruso"));
        verifyNoInteractions(notifier);
    }

    @Test
    void conLaPartidaIniciadaElChatDeLaSalaYaNoEstaDisponibleYSeBorra() {
        chat.enviarSala("s1", "ana", "antes de jugar");
        salas.iniciarPartida("s1");

        assertThrows(ChatNoDisponibleException.class, () -> chat.enviarSala("s1", "ana", "durante"));
        assertThrows(ChatNoDisponibleException.class, () -> chat.historialSala("s1", "ana"));
        assertTrue(mensajes.historial("salas/s1").isEmpty());
        verify(notifier, times(1)).mensajeNuevo(any());
    }

    @Test
    void unaSalaQueNoExisteNoTieneChat() {
        assertThrows(ChatNoDisponibleException.class, () -> chat.enviarSala("no-existe", "ana", "hola"));
        assertThrows(ChatNoDisponibleException.class, () -> chat.historialSala("no-existe", "ana"));
    }

    @Test
    void unMensajeInvalidoNoSeGuardaNiSeAvisa() {
        assertThrows(IllegalArgumentException.class, () -> chat.enviarSala("s1", "ana", "   "));
        assertThrows(IllegalArgumentException.class, () -> chat.enviarSala("s1", "ana", "x".repeat(MensajeChat.MAX_LONGITUD + 1)));

        assertTrue(chat.historialSala("s1", "ana").isEmpty());
        verifyNoInteractions(notifier);
    }

    @Test
    void elHistorialPideUserId() {
        assertThrows(IllegalArgumentException.class, () -> chat.historialSala("s1", null));
        assertThrows(IllegalArgumentException.class, () -> chat.historialSala("s1", " "));
        assertThrows(IllegalArgumentException.class, () -> chat.historialPartida("m1", null));
        assertThrows(IllegalArgumentException.class, () -> chat.historialEquipo("m1", " "));
    }

    @Test
    void cerrarBorraElHistorialDeLaSala() {
        chat.enviarSala("s1", "ana", "hola");

        chat.cerrarSala("s1");

        assertTrue(chat.historialSala("s1", "ana").isEmpty());
    }

    // Global

    @Test
    void cualquierUsuarioExistenteHablaEnElGlobal() {
        MensajeChatDto dto = chat.enviarGlobal("caro", "hola a todos");

        assertEquals("global", dto.canal());
        verify(notifier).mensajeNuevo(dto);
        assertEquals(List.of("hola a todos"), textos(chat.historialGlobal()));
    }

    @Test
    void unUsuarioQueNoExisteNoHablaEnElGlobal() {
        assertThrows(IllegalArgumentException.class, () -> chat.enviarGlobal("fantasma", "hola"));
        assertThrows(IllegalArgumentException.class, () -> chat.enviarGlobal(null, "hola"));
        verifyNoInteractions(notifier);
    }

    @Test
    void losMensajesDelGlobalCaducanALaHora() {
        chat.enviarGlobal("ana", "viejo");
        reloj.avanzar(ChatService.CADUCIDAD_GLOBAL.minusMinutes(30));
        chat.enviarGlobal("ana", "nuevo");
        reloj.avanzar(Duration.ofMinutes(30));

        assertEquals(List.of("nuevo"), textos(chat.historialGlobal()));
    }

    // Partida y equipo

    @Test
    void todosLosDeLaPartidaHablanEnSuCanal() {
        chat.partidaIniciada("m1", EQUIPOS);

        MensajeChatDto dto = chat.enviarPartida("m1", "ana", "suerte");

        assertEquals("partidas/m1", dto.canal());
        assertEquals(List.of("suerte"), textos(chat.historialPartida("m1", "caro")));
    }

    @Test
    void elChatDeEquipoSoloLoVeSuEquipo() {
        chat.partidaIniciada("m1", EQUIPOS);

        MensajeChatDto dto = chat.enviarEquipo("m1", "ana", "la llave esta arriba");

        assertEquals("partidas/m1/equipos/e1", dto.canal());
        assertEquals(List.of("la llave esta arriba"), textos(chat.historialEquipo("m1", "beto")));
        assertTrue(chat.historialEquipo("m1", "caro").isEmpty());
    }

    @Test
    void quienNoJuegaLaPartidaNoEscribeNiLee() {
        chat.partidaIniciada("m1", EQUIPOS);

        assertThrows(JugadorFueraDeSalaException.class, () -> chat.enviarPartida("m1", "intruso", "hola"));
        assertThrows(JugadorFueraDeSalaException.class, () -> chat.enviarEquipo("m1", "intruso", "hola"));
        assertThrows(JugadorFueraDeSalaException.class, () -> chat.historialPartida("m1", "intruso"));
        assertThrows(JugadorFueraDeSalaException.class, () -> chat.historialEquipo("m1", "intruso"));
    }

    @Test
    void unaPartidaQueNoExisteNoTieneChat() {
        assertThrows(ChatNoDisponibleException.class, () -> chat.enviarPartida("no-existe", "ana", "hola"));
        assertThrows(ChatNoDisponibleException.class, () -> chat.enviarEquipo("no-existe", "ana", "hola"));
    }

    @Test
    void alIniciarLaPartidaSeBorraElChatDeLaSala() {
        chat.enviarSala("s1", "ana", "antes");

        chat.partidaIniciada("s1", EQUIPOS);

        assertTrue(mensajes.historial("salas/s1").isEmpty());
    }

    @Test
    void alTerminarLaPartidaSeOlvidanSusCanales() {
        chat.partidaIniciada("m1", EQUIPOS);
        chat.enviarPartida("m1", "ana", "todos");
        chat.enviarEquipo("m1", "ana", "equipo");

        chat.partidaTerminada("m1");

        assertTrue(mensajes.historial("partidas/m1").isEmpty());
        assertTrue(mensajes.historial("partidas/m1/equipos/e1").isEmpty());
        assertThrows(ChatNoDisponibleException.class, () -> chat.enviarPartida("m1", "ana", "tarde"));
    }

    @Test
    void terminarUnaPartidaDesconocidaNoFalla() {
        assertDoesNotThrow(() -> chat.partidaTerminada("no-existe"));
    }

    // Anti-spam

    @Test
    void quienEnviaDemasiadoRapidoEsFrenadoSumandoTodosLosCanalesYLuegoPuedeSeguir() {
        for (int i = 0; i < LimiteEnvios.MAX_MENSAJES - 1; i++) chat.enviarGlobal("ana", "m" + i);
        chat.enviarSala("s1", "ana", "ultimo permitido");

        assertThrows(DemasiadosMensajesException.class, () -> chat.enviarGlobal("ana", "de mas"));
        assertEquals(LimiteEnvios.MAX_MENSAJES - 1, chat.historialGlobal().size());

        // Otro usuario no se ve afectado
        assertDoesNotThrow(() -> chat.enviarGlobal("beto", "hola"));

        reloj.avanzar(LimiteEnvios.VENTANA);
        assertDoesNotThrow(() -> chat.enviarGlobal("ana", "ya puedo"));
    }

    @Test
    void unMensajeRechazadoNoGastaElLimite() {
        for (int i = 0; i < LimiteEnvios.MAX_MENSAJES * 2; i++) {
            assertThrows(IllegalArgumentException.class, () -> chat.enviarGlobal("ana", "  "));
        }

        assertDoesNotThrow(() -> chat.enviarGlobal("ana", "hola"));
    }

    /** Salas en espera con sus jugadores; las que no estan aqui no existen. */
    static class SalasFalsas implements SalaEsperaConsulta {
        private final Map<String, Set<String>> jugadores = new HashMap<>();
        private final Set<String> iniciadas = new HashSet<>();

        void enEspera(String salaId, String... usuarios) {
            jugadores.put(salaId, Set.of(usuarios));
        }

        void iniciarPartida(String salaId) {
            iniciadas.add(salaId);
        }

        @Override
        public boolean enEspera(String salaId) {
            return jugadores.containsKey(salaId) && !iniciadas.contains(salaId);
        }

        @Override
        public boolean contieneJugador(String salaId, String userId) {
            return jugadores.getOrDefault(salaId, Set.of()).contains(userId);
        }
    }

    static class RelojMovible extends Clock {
        private Instant ahora;

        RelojMovible(Instant inicio) {
            ahora = inicio;
        }

        void avanzar(Duration tiempo) {
            ahora = ahora.plus(tiempo);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
