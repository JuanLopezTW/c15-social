package co.eci.c15.estadisticas.infrastructure.events;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.estadisticas.application.EstadisticasService;
import co.eci.c15.estadisticas.application.RankingNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Un fallo guardando estadisticas no debe impedir que la partida termine para los demas modulos. */
@Component
public class EstadisticasPartidaListener {

    private static final Logger log = LoggerFactory.getLogger(EstadisticasPartidaListener.class);

    private final EstadisticasService estadisticas;
    private final RankingNotifier notifier;

    public EstadisticasPartidaListener(EstadisticasService estadisticas, RankingNotifier notifier) {
        this.estadisticas = estadisticas;
        this.notifier = notifier;
    }

    /** Se avisa despues de guardar (la transaccion ya hizo commit), asi quien recargue ya ve la partida. */
    @EventListener
    public void onPartidaTerminada(MatchFinishedEvent event) {
        try {
            if (estadisticas.partidaTerminada(event)) notifier.rankingActualizado(event.matchId());
        } catch (RuntimeException e) {
            log.error("No se pudieron guardar las estadisticas de la partida {}", event.matchId(), e);
        }
    }
}
