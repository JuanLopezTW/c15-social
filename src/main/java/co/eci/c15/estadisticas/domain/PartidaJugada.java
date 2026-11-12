package co.eci.c15.estadisticas.domain;

import java.time.Instant;

/** Una partida terminada, vista por uno de sus jugadores. */
public record PartidaJugada(String matchId, String userId, String equipoId, ResultadoPartida resultado,
                            int puntajeEquipo, Instant jugadaEn) {
}
