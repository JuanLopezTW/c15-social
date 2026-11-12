package co.eci.c15.estadisticas.application;

import co.eci.c15.estadisticas.domain.EstadisticaJugador;
import co.eci.c15.estadisticas.domain.PartidaJugada;

import java.time.Instant;
import java.util.List;

public record EstadisticasJugadorDto(String userId, long partidas, long victorias, long empates, long derrotas,
                                     long puntos, int porcentajeVictorias, List<PartidaDto> ultimasPartidas) {

    public record PartidaDto(String matchId, String resultado, int puntajeEquipo, Instant jugadaEn) {
    }

    static EstadisticasJugadorDto from(EstadisticaJugador e, List<PartidaJugada> ultimas) {
        return new EstadisticasJugadorDto(e.userId(), e.partidas(), e.victorias(), e.empates(), e.derrotas(),
                e.puntos(), e.porcentajeVictorias(),
                ultimas.stream()
                        .map(p -> new PartidaDto(p.matchId(), p.resultado().name(), p.puntajeEquipo(), p.jugadaEn()))
                        .toList());
    }
}
