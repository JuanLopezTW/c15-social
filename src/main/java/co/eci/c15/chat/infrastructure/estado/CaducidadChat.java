package co.eci.c15.chat.infrastructure.estado;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Cuanto vive cada cosa del chat en Redis (regla del profe: minutos, nunca mas de una hora). Cada envio
 * renueva la caducidad del canal, asi que un canal se borra solo cuando lleva ese tiempo sin mensajes.
 */
@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
public class CaducidadChat {

    /** El global ya filtra los mensajes de mas de una hora: no tiene sentido guardar mas. */
    static final Duration GLOBAL = Duration.ofHours(1);
    /** Una sala de espera sin mensajes en este tiempo pierde su historial (la sala no se cierra). */
    static final Duration SALA = Duration.ofMinutes(30);

    private final Duration partida;

    public CaducidadChat(@Value("${c15.estado.margen:10m}") Duration margen) {
        this.partida = PartidaIniciadaEvent.DURACION_MAXIMA.plus(margen);
    }

    /** Partida y equipos: lo mas que puede durar una partida, mas el margen. */
    public Duration partida() {
        return partida;
    }

    public Duration deCanal(String canal) {
        if (canal.startsWith("partidas/")) return partida;
        if (canal.startsWith("salas/")) return SALA;
        return GLOBAL;
    }
}
