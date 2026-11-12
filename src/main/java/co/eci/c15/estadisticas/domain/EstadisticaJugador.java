package co.eci.c15.estadisticas.domain;

/** Totales de un jugador. */
public record EstadisticaJugador(String userId, long partidas, long victorias, long empates, long derrotas) {

    public static EstadisticaJugador sinPartidas(String userId) {
        return new EstadisticaJugador(userId, 0, 0, 0, 0);
    }

    public long puntos() {
        return victorias * ResultadoPartida.VICTORIA.puntos() + empates * ResultadoPartida.EMPATE.puntos();
    }

    public int porcentajeVictorias() {
        return partidas == 0 ? 0 : Math.round(100f * victorias / partidas);
    }
}
