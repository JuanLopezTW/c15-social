package co.eci.c15.estadisticas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadisticaJugadorTest {

    @Test
    void victoriaValeTresEmpateUnoYDerrotaCero() {
        assertEquals(3 * 2 + 1, new EstadisticaJugador("ana", 6, 2, 1, 3).puntos());
    }

    @Test
    void elPorcentajeSeRedondea() {
        assertEquals(33, new EstadisticaJugador("ana", 3, 1, 0, 2).porcentajeVictorias());
        assertEquals(67, new EstadisticaJugador("ana", 3, 2, 0, 1).porcentajeVictorias());
    }

    @Test
    void sinPartidasTodoEsCeroYNoDivideEntreCero() {
        EstadisticaJugador nuevo = EstadisticaJugador.sinPartidas("ana");

        assertEquals(0, nuevo.puntos());
        assertEquals(0, nuevo.porcentajeVictorias());
    }
}
