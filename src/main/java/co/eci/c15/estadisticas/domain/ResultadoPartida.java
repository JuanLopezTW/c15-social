package co.eci.c15.estadisticas.domain;

/** Como le fue a un jugador en una partida y cuanto suma al ranking. */
public enum ResultadoPartida {
    VICTORIA(3), EMPATE(1), DERROTA(0);

    private final int puntos;

    ResultadoPartida(int puntos) {
        this.puntos = puntos;
    }

    public int puntos() {
        return puntos;
    }
}
