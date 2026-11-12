package co.eci.c15.chat.infrastructure.web;

import co.eci.c15.chat.application.ChatNotifier;
import co.eci.c15.chat.application.ChatService;
import co.eci.c15.chat.application.SalaEsperaConsulta;
import co.eci.c15.chat.infrastructure.estado.EquiposDePartidaEnMemoria;
import co.eci.c15.chat.infrastructure.estado.LimiteEnviosEnMemoria;
import co.eci.c15.chat.infrastructure.persistence.ChatEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    private MockMvc mvc;
    private ChatService chat;
    private final AtomicBoolean enEspera = new AtomicBoolean(true);

    @BeforeEach
    void setUp() {
        SalaEsperaConsulta salas = new SalaEsperaConsulta() {
            @Override
            public boolean enEspera(String salaId) {
                return "s1".equals(salaId) && enEspera.get();
            }

            @Override
            public boolean contieneJugador(String salaId, String userId) {
                return "ana".equals(userId) || "beto".equals(userId);
            }
        };
        ChatNotifier notifier = m -> { };
        chat = new ChatService(new ChatEnMemoria(), salas, Set.of("ana", "beto", "caro")::contains, notifier,
                new LimiteEnviosEnMemoria(), new EquiposDePartidaEnMemoria());
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(chat)).build();
    }

    private ResultActions enviar(String ruta, String userId, String texto) throws Exception {
        return mvc.perform(post(ruta).principal(() -> userId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"" + texto + "\"}"));
    }

    @Test
    void enviarDevuelve201ConElMensaje() throws Exception {
        enviar("/api/chat/salas/s1", "ana", "hola")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.canal").value("salas/s1"))
                .andExpect(jsonPath("$.userId").value("ana"))
                .andExpect(jsonPath("$.texto").value("hola"))
                .andExpect(jsonPath("$.enviadoEn").isNotEmpty());
    }

    @Test
    void elHistorialTraeLosMensajesDeLaSala() throws Exception {
        enviar("/api/chat/salas/s1", "ana", "hola");
        enviar("/api/chat/salas/s1", "beto", "que mas");

        mvc.perform(get("/api/chat/salas/s1").principal(() -> "beto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].texto").value("hola"))
                .andExpect(jsonPath("$[1].userId").value("beto"));
    }

    @Test
    void mensajeVacioDevuelve400() throws Exception {
        enviar("/api/chat/salas/s1", "ana", "  ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void jugadorAjenoALaSalaDevuelve403() throws Exception {
        enviar("/api/chat/salas/s1", "intruso", "hola").andExpect(status().isForbidden());
        mvc.perform(get("/api/chat/salas/s1").principal(() -> "intruso")).andExpect(status().isForbidden());
    }

    @Test
    void fueraDeLaSalaDeEsperaDevuelve409() throws Exception {
        enEspera.set(false);

        enviar("/api/chat/salas/s1", "ana", "hola")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").isNotEmpty());
        mvc.perform(get("/api/chat/salas/s1").principal(() -> "ana")).andExpect(status().isConflict());
    }

    @Test
    void elGlobalNoPideUserIdParaLeerYSiParaEscribir() throws Exception {
        enviar("/api/chat/global", "caro", "hola").andExpect(status().isCreated())
                .andExpect(jsonPath("$.canal").value("global"));
        enviar("/api/chat/global", "fantasma", "hola").andExpect(status().isBadRequest());

        mvc.perform(get("/api/chat/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void losCanalesDeLaPartidaSeparanEquipos() throws Exception {
        chat.partidaIniciada("m1", Map.of("e1", List.of("ana"), "e2", List.of("caro")));

        enviar("/api/chat/partidas/m1", "ana", "para todos").andExpect(status().isCreated())
                .andExpect(jsonPath("$.canal").value("partidas/m1"));
        enviar("/api/chat/partidas/m1/equipo", "ana", "secreto").andExpect(status().isCreated())
                .andExpect(jsonPath("$.canal").value("partidas/m1/equipos/e1"));

        mvc.perform(get("/api/chat/partidas/m1").principal(() -> "caro"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/chat/partidas/m1/equipo").principal(() -> "caro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void demasiadosMensajesDevuelve429() throws Exception {
        for (int i = 0; i < 5; i++) enviar("/api/chat/global", "ana", "m" + i).andExpect(status().isCreated());

        enviar("/api/chat/global", "ana", "de mas")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }
}
