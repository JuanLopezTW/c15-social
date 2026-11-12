package co.eci.c15.estadisticas.application;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.estadisticas.domain.EstadisticaJugador;
import co.eci.c15.estadisticas.domain.EstadisticasRepository;
import co.eci.c15.estadisticas.domain.PartidaJugada;
import co.eci.c15.estadisticas.domain.ResultadoPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EstadisticasServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");
    private static final Map<String, List<String>> EQUIPOS = Map.of("e1", List.of("ana", "beto"), "e2", List.of("caro"));

    private EstadisticasRepository repositorio;
    private EstadisticasService estadisticas;

    @BeforeEach
    void setUp() {
        repositorio = mock(EstadisticasRepository.class);
        estadisticas = new EstadisticasService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @SuppressWarnings("unchecked")
    private Map<String, ResultadoPartida> resultadosGuardados() {
        ArgumentCaptor<List<PartidaJugada>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(repositorio).guardarTodas(guardadas.capture());
        return guardadas.getValue().stream().collect(Collectors.toMap(PartidaJugada::userId, PartidaJugada::resultado));
    }

    @Test
    void elEquipoGanadorSumaVictoriaYElRestoDerrota() {
        estadisticas.partidaTerminada(new MatchFinishedEvent("m1", "TIME_UP", "e1", Map.of("e1", 2, "e2", 1), EQUIPOS));

        assertEquals(Map.of("ana", ResultadoPartida.VICTORIA, "beto", ResultadoPartida.VICTORIA,
                "caro", ResultadoPartida.DERROTA), resultadosGuardados());
    }

    @Test
    void sinGanadorEmpatanLosDelMejorPuntajeYElRestoPierde() {
        Map<String, List<String>> tres = Map.of("e1", List.of("ana"), "e2", List.of("beto"), "e3", List.of("caro"));

        estadisticas.partidaTerminada(new MatchFinishedEvent("m1", "TIME_UP", null, Map.of("e1", 2, "e2", 2, "e3", 1), tres));

        assertEquals(Map.of("ana", ResultadoPartida.EMPATE, "beto", ResultadoPartida.EMPATE,
                "caro", ResultadoPartida.DERROTA), resultadosGuardados());
    }

    @Test
    void guardaElPuntajeDelEquipoYLaFecha() {
        estadisticas.partidaTerminada(new MatchFinishedEvent("m1", "TIME_UP", "e1", Map.of("e1", 3),
                Map.of("e1", List.of("ana"))));

        verify(repositorio).guardarTodas(List.of(
                new PartidaJugada("m1", "ana", "e1", ResultadoPartida.VICTORIA, 3, AHORA)));
    }

    @Test
    void otraInstanciaQueNoVioIniciarLaPartidaIgualLaRegistra() {
        EstadisticasService otra = new EstadisticasService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));

        assertTrue(otra.partidaTerminada(new MatchFinishedEvent("m1", "TIME_UP", "e1", Map.of("e1", 1, "e2", 0), EQUIPOS)));

        assertEquals(3, resultadosGuardados().size());
    }

    @Test
    void unaPartidaSinEquiposSeIgnora() {
        assertFalse(estadisticas.partidaTerminada(new MatchFinishedEvent("m1", "TIME_UP", "e1", Map.of("e1", 1), Map.of())));
        assertFalse(estadisticas.partidaTerminada(new MatchFinishedEvent("m1", "TIME_UP", "e1", Map.of("e1", 1), null)));

        verify(repositorio, never()).guardarTodas(any());
    }

    @Test
    void siElEventoLlegaDosVecesSoloCuentaUna() {
        MatchFinishedEvent fin = new MatchFinishedEvent("m1", "TIME_UP", "e1", Map.of("e1", 1, "e2", 0), EQUIPOS);
        when(repositorio.existePartida("m1")).thenReturn(false, true);

        assertTrue(estadisticas.partidaTerminada(fin));
        assertFalse(estadisticas.partidaTerminada(fin));

        verify(repositorio, times(1)).guardarTodas(any());
    }

    @Test
    void unaPartidaYaGuardadaNoSeRepite() {
        when(repositorio.existePartida("m1")).thenReturn(true);

        assertFalse(estadisticas.partidaTerminada(new MatchFinishedEvent("m1", "TIME_UP", "e1", Map.of("e1", 1), EQUIPOS)));

        verify(repositorio, never()).guardarTodas(any());
    }

    @Test
    void elRankingNumeraLasPosicionesYLimitaElTamano() {
        when(repositorio.ranking(anyInt())).thenReturn(List.of(
                new EstadisticaJugador("ana", 2, 2, 0, 0), new EstadisticaJugador("beto", 2, 0, 1, 1)));

        List<JugadorRankingDto> ranking = estadisticas.ranking(1000);

        verify(repositorio).ranking(EstadisticasService.MAX_RANKING);
        assertEquals(new JugadorRankingDto(1, "ana", 2, 2, 0, 0, 6), ranking.get(0));
        assertEquals(new JugadorRankingDto(2, "beto", 2, 0, 1, 1, 1), ranking.get(1));
    }

    @Test
    void unLimiteInvalidoSeAjusta() {
        when(repositorio.ranking(anyInt())).thenReturn(List.of());

        estadisticas.ranking(0);

        verify(repositorio).ranking(1);
    }

    @Test
    void lasEstadisticasDeUnJugadorTraenTotalesYUltimasPartidas() {
        when(repositorio.deJugador("ana")).thenReturn(Optional.of(new EstadisticaJugador("ana", 2, 1, 1, 0)));
        when(repositorio.ultimasDeJugador("ana", EstadisticasService.ULTIMAS_PARTIDAS)).thenReturn(List.of(
                new PartidaJugada("m2", "ana", "e1", ResultadoPartida.EMPATE, 1, AHORA)));

        EstadisticasJugadorDto dto = estadisticas.deJugador("ana");

        assertEquals(4, dto.puntos());
        assertEquals(50, dto.porcentajeVictorias());
        assertEquals(List.of(new EstadisticasJugadorDto.PartidaDto("m2", "EMPATE", 1, AHORA)), dto.ultimasPartidas());
    }

    @Test
    void unJugadorSinPartidasDevuelveCeros() {
        when(repositorio.deJugador("nuevo")).thenReturn(Optional.empty());

        EstadisticasJugadorDto dto = estadisticas.deJugador("nuevo");

        assertEquals(0, dto.partidas());
        assertEquals(0, dto.puntos());
        assertTrue(dto.ultimasPartidas().isEmpty());
    }
}
