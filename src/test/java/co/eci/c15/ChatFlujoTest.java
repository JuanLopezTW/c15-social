package co.eci.c15;

import co.eci.c15.chat.application.SalaEsperaConsulta;
import co.eci.c15.chat.domain.ChatRepository;
import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.common.events.SalaCerradaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los 4 chats con los usuarios y los eventos reales; lo que sabe el servicio de salas va simulado. */
@SpringBootTest
@AutoConfigureMockMvc
class ChatFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEventPublisher eventos;

    @Autowired
    private ChatRepository historial;

    @MockitoBean
    private SalaEsperaConsulta salas;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void losJugadoresDeLaSalaEscribenYLeenElHistorialEnOrden() throws Exception {
        salaEnEspera("s1", "ana1", "beto1");
        String ruta = "/api/chat/salas/s1";

        enviar(ruta, "ana1", "hola").andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.canal").value("salas/s1"))
                .andExpect(jsonPath("$.userId").value("ana1"))
                .andExpect(jsonPath("$.enviadoEn").isString());
        enviar(ruta, "beto1", "listo?").andExpect(status().isCreated());

        mvc.perform(get(ruta).param("userId", "beto1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].texto").value("hola"))
                .andExpect(jsonPath("$[1].texto").value("listo?"));
    }

    @Test
    void quienNoEstaEnLaSalaNoLeeNiEscribe() throws Exception {
        salaEnEspera("s2", "ana2");

        enviar("/api/chat/salas/s2", "intruso", "hola").andExpect(status().isForbidden());
        mvc.perform(get("/api/chat/salas/s2").param("userId", "intruso")).andExpect(status().isForbidden());
    }

    @Test
    void siLaSalaYaNoEstaEnEsperaSuChatSeCierraYSeOlvida() throws Exception {
        salaEnEspera("s3", "ana3");
        enviar("/api/chat/salas/s3", "ana3", "hola").andExpect(status().isCreated());

        when(salas.enEspera("s3")).thenReturn(false);

        enviar("/api/chat/salas/s3", "ana3", "sigo aqui").andExpect(status().isConflict());
        assertTrue(historial.historial("salas/s3").isEmpty());
    }

    @Test
    void siSeCierraLaSalaSeOlvidaSuHistorial() throws Exception {
        salaEnEspera("s4", "ana4");
        enviar("/api/chat/salas/s4", "ana4", "hola").andExpect(status().isCreated());

        eventos.publishEvent(new SalaCerradaEvent("s4"));

        assertTrue(historial.historial("salas/s4").isEmpty());
    }

    @Test
    void unMensajeVacioODemasiadoLargoSeRechaza() throws Exception {
        salaEnEspera("s5", "ana5");

        enviar("/api/chat/salas/s5", "ana5", "   ").andExpect(status().isBadRequest());
        enviar("/api/chat/salas/s5", "ana5", "x".repeat(201)).andExpect(status().isBadRequest());
    }

    @Test
    void elGlobalEsParaLosUsuariosRegistrados() throws Exception {
        mvc.perform(post("/api/perfil").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"auth0|chat-global\",\"username\":\"chat_global\"}"))
                .andExpect(status().isOk());

        enviar("/api/chat/global", "auth0|chat-global", "hola mundo").andExpect(status().isCreated());
        enviar("/api/chat/global", "no-registrado", "hola").andExpect(status().isBadRequest());

        mvc.perform(get("/api/chat/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.texto == 'hola mundo')]", hasSize(1)));
    }

    @Test
    void alIniciarLaPartidaSeAbrenSuChatYElDelEquipoYAlTerminarSeCierran() throws Exception {
        eventos.publishEvent(new PartidaIniciadaEvent("chat-m1",
                Map.of("chat-e1", List.of("ana6", "beto6"), "chat-e2", List.of("caro6", "dani6"))));
        String general = "/api/chat/partidas/chat-m1";
        String equipo = general + "/equipo";

        enviar(general, "ana6", "suerte").andExpect(status().isCreated());
        enviar(equipo, "ana6", "secreto").andExpect(status().isCreated());

        mvc.perform(get(general).param("userId", "caro6")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(equipo).param("userId", "beto6")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(equipo).param("userId", "caro6")).andExpect(jsonPath("$", hasSize(0)));

        eventos.publishEvent(new MatchFinishedEvent("chat-m1", "TIME_UP", null, Map.of("chat-e1", 0, "chat-e2", 0), Map.of()));

        enviar(general, "ana6", "tarde").andExpect(status().isConflict());
        assertTrue(historial.historial("partidas/chat-m1").isEmpty());
    }

    private void salaEnEspera(String salaId, String... jugadores) {
        when(salas.enEspera(salaId)).thenReturn(true);
        for (String j : jugadores) when(salas.contieneJugador(salaId, j)).thenReturn(true);
    }

    private ResultActions enviar(String ruta, String userId, String texto) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("userId", userId, "texto", texto))));
    }
}
