package co.eci.c15;

import co.eci.c15.common.events.MatchFinishedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Al terminar una partida, sus jugadores aparecen en el ranking y en sus estadisticas. */
@SpringBootTest
@AutoConfigureMockMvc
class EstadisticasFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEventPublisher eventos;

    @Test
    void unaPartidaTerminadaSumaAlRankingYALasEstadisticas() throws Exception {
        eventos.publishEvent(new MatchFinishedEvent("stats-m1", "TIME_UP", "stats-e1", Map.of("stats-e1", 2, "stats-e2", 1),
                Map.of("stats-e1", List.of("stats-ana"), "stats-e2", List.of("stats-beto"))));

        mvc.perform(get("/api/estadisticas/jugadores/stats-ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partidas").value(1))
                .andExpect(jsonPath("$.victorias").value(1))
                .andExpect(jsonPath("$.puntos").value(3))
                .andExpect(jsonPath("$.ultimasPartidas[0].resultado").value("VICTORIA"))
                .andExpect(jsonPath("$.ultimasPartidas[0].jugadaEn").isString());
        mvc.perform(get("/api/estadisticas/jugadores/stats-beto"))
                .andExpect(jsonPath("$.derrotas").value(1))
                .andExpect(jsonPath("$.puntos").value(0));
        mvc.perform(get("/api/estadisticas/ranking").param("limite", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == 'stats-ana')]", hasSize(1)));
    }

    @Test
    void unJugadorSinPartidasVeCeros() throws Exception {
        mvc.perform(get("/api/estadisticas/jugadores/nunca-jugo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partidas").value(0))
                .andExpect(jsonPath("$.ultimasPartidas", hasSize(0)));
    }
}
