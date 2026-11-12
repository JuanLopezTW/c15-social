package co.eci.c15.estadisticas.infrastructure.web;

import co.eci.c15.estadisticas.application.EstadisticasJugadorDto;
import co.eci.c15.estadisticas.application.EstadisticasService;
import co.eci.c15.estadisticas.application.JugadorRankingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstadisticasControllerTest {

    private final EstadisticasService estadisticas = mock(EstadisticasService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new EstadisticasController(estadisticas)).build();
    }

    @Test
    void elRankingUsaDiezPorDefecto() throws Exception {
        when(estadisticas.ranking(10)).thenReturn(List.of(new JugadorRankingDto(1, "ana", 2, 2, 0, 0, 6)));

        mvc.perform(get("/api/estadisticas/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].posicion").value(1))
                .andExpect(jsonPath("$[0].userId").value("ana"))
                .andExpect(jsonPath("$[0].puntos").value(6));
    }

    @Test
    void elRankingAceptaOtroLimite() throws Exception {
        when(estadisticas.ranking(3)).thenReturn(List.of());

        mvc.perform(get("/api/estadisticas/ranking").param("limite", "3")).andExpect(status().isOk());

        verify(estadisticas).ranking(3);
    }

    @Test
    void devuelveLasEstadisticasDelJugador() throws Exception {
        when(estadisticas.deJugador("ana")).thenReturn(new EstadisticasJugadorDto("ana", 2, 1, 1, 0, 4, 50, List.of()));

        mvc.perform(get("/api/estadisticas/jugadores/ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntos").value(4))
                .andExpect(jsonPath("$.porcentajeVictorias").value(50));
    }
}
