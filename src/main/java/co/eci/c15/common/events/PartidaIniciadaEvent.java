package co.eci.c15.common.events;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * La partida arranco. Cada equipo (equipoId) trae la lista de sus jugadores (userId), y {@code duracion}
 * es lo que dura la partida (la elige el anfitrion al crear la sala): nadie mas la lee de su configuracion.
 */
public record PartidaIniciadaEvent(String matchId, Map<String, List<String>> equipos, Duration duracion) {

    /** Lo mas que puede durar una partida: con esto se calcula cuanto vive su estado en Redis. */
    public static final Duration DURACION_MAXIMA = Duration.ofMinutes(15);
    public static final Duration DURACION_DEFECTO = Duration.ofMinutes(5);

    public PartidaIniciadaEvent {
        if (duracion == null || duracion.isNegative() || duracion.isZero() || duracion.compareTo(DURACION_MAXIMA) > 0) {
            throw new IllegalArgumentException("La duracion de la partida debe estar entre 0 y " + DURACION_MAXIMA);
        }
    }

    /** Con la duracion por defecto (pruebas y endpoints de desarrollo). */
    public PartidaIniciadaEvent(String matchId, Map<String, List<String>> equipos) {
        this(matchId, equipos, DURACION_DEFECTO);
    }
}
