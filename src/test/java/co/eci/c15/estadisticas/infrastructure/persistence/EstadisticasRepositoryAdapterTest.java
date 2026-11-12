package co.eci.c15.estadisticas.infrastructure.persistence;

import co.eci.c15.estadisticas.domain.EstadisticaJugador;
import co.eci.c15.estadisticas.domain.PartidaJugada;
import co.eci.c15.estadisticas.domain.ResultadoPartida;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(EstadisticasRepositoryAdapter.class)
class EstadisticasRepositoryAdapterTest {

    private static final Instant T0 = Instant.parse("2026-11-02T15:00:00Z");

    @Autowired
    private EstadisticasRepositoryAdapter adapter;

    private void guardar(String matchId, String userId, ResultadoPartida resultado, Instant cuando) {
        adapter.guardarTodas(List.of(new PartidaJugada(matchId, userId, "e1", resultado, 2, cuando)));
    }

    @Test
    void sumaLosTotalesDeUnJugador() {
        guardar("m1", "ana", ResultadoPartida.VICTORIA, T0);
        guardar("m2", "ana", ResultadoPartida.EMPATE, T0);
        guardar("m3", "ana", ResultadoPartida.DERROTA, T0);
        guardar("m1", "beto", ResultadoPartida.DERROTA, T0);

        assertEquals(new EstadisticaJugador("ana", 3, 1, 1, 1), adapter.deJugador("ana").orElseThrow());
        assertTrue(adapter.deJugador("nadie").isEmpty());
    }

    @Test
    void elRankingOrdenaPorPuntosLuegoVictoriasLuegoNombreYRespetaElLimite() {
        // caro: 3 puntos con 1 victoria; beto: 3 puntos con 3 empates; ana: 4 puntos; dani: 0
        guardar("m1", "caro", ResultadoPartida.VICTORIA, T0);
        for (String m : List.of("m1", "m2", "m3")) guardar(m, "beto", ResultadoPartida.EMPATE, T0);
        guardar("m1", "ana", ResultadoPartida.VICTORIA, T0);
        guardar("m2", "ana", ResultadoPartida.EMPATE, T0);
        guardar("m1", "dani", ResultadoPartida.DERROTA, T0);

        assertEquals(List.of("ana", "caro", "beto", "dani"),
                adapter.ranking(10).stream().map(EstadisticaJugador::userId).toList());
        assertEquals(List.of("ana", "caro"),
                adapter.ranking(2).stream().map(EstadisticaJugador::userId).toList());
    }

    @Test
    void lasUltimasPartidasVanDeLaMasNuevaALaMasVieja() {
        guardar("m1", "ana", ResultadoPartida.VICTORIA, T0);
        guardar("m2", "ana", ResultadoPartida.DERROTA, T0.plusSeconds(60));
        guardar("m3", "ana", ResultadoPartida.EMPATE, T0.plusSeconds(120));

        List<PartidaJugada> ultimas = adapter.ultimasDeJugador("ana", 2);

        assertEquals(List.of("m3", "m2"), ultimas.stream().map(PartidaJugada::matchId).toList());
        assertEquals(new PartidaJugada("m3", "ana", "e1", ResultadoPartida.EMPATE, 2, T0.plusSeconds(120)), ultimas.get(0));
    }

    @Test
    void sabeSiUnaPartidaYaEstaRegistrada() {
        guardar("m1", "ana", ResultadoPartida.VICTORIA, T0);

        assertTrue(adapter.existePartida("m1"));
        assertFalse(adapter.existePartida("m2"));
    }

    @Test
    void unJugadorCuentaUnaSolaVezPorPartida() {
        guardar("m1", "ana", ResultadoPartida.VICTORIA, T0);
        guardar("m1", "ana", ResultadoPartida.VICTORIA, T0);

        assertEquals(1, adapter.deJugador("ana").orElseThrow().partidas());
    }
}
