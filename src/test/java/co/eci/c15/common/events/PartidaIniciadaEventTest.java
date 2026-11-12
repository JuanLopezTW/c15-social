package co.eci.c15.common.events;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PartidaIniciadaEventTest {

    private static final Map<String, List<String>> EQUIPOS = Map.of("e1", List.of("ana"), "e2", List.of("beto"));

    @Test
    void llevaLaDuracionQueEligioElAnfitrion() {
        assertEquals(Duration.ofMinutes(10), new PartidaIniciadaEvent("m1", EQUIPOS, Duration.ofMinutes(10)).duracion());
        assertEquals(PartidaIniciadaEvent.DURACION_MAXIMA,
                new PartidaIniciadaEvent("m1", EQUIPOS, PartidaIniciadaEvent.DURACION_MAXIMA).duracion());
    }

    @Test
    void sinDuracionUsaLaDeDefecto() {
        assertEquals(PartidaIniciadaEvent.DURACION_DEFECTO, new PartidaIniciadaEvent("m1", EQUIPOS).duracion());
    }

    @Test
    void unaDuracionFueraDeRangoEsInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new PartidaIniciadaEvent("m1", EQUIPOS, null));
        assertThrows(IllegalArgumentException.class, () -> new PartidaIniciadaEvent("m1", EQUIPOS, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new PartidaIniciadaEvent("m1", EQUIPOS, Duration.ofMinutes(-1)));
        assertThrows(IllegalArgumentException.class,
                () -> new PartidaIniciadaEvent("m1", EQUIPOS, PartidaIniciadaEvent.DURACION_MAXIMA.plusSeconds(1)));
    }
}
