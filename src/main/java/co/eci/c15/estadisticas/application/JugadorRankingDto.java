package co.eci.c15.estadisticas.application;

import co.eci.c15.estadisticas.domain.EstadisticaJugador;

public record JugadorRankingDto(int posicion, String userId, long partidas, long victorias, long empates,
                                long derrotas, long puntos) {

    static JugadorRankingDto from(int posicion, EstadisticaJugador e) {
        return new JugadorRankingDto(posicion, e.userId(), e.partidas(), e.victorias(), e.empates(), e.derrotas(), e.puntos());
    }
}
