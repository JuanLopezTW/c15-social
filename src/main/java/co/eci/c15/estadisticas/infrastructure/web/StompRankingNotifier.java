package co.eci.c15.estadisticas.infrastructure.web;

import co.eci.c15.estadisticas.application.RankingNotifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publica en /topic/estadisticas/ranking solo un aviso con el matchId: cada cliente vuelve a pedir el
 * ranking con su propio limite y sus estadisticas, en vez de mandarle a todos una lista que quiza no usa.
 */
@Component
public class StompRankingNotifier implements RankingNotifier {

    static final String TOPICO = "/topic/estadisticas/ranking";

    private final SimpMessagingTemplate messaging;

    public StompRankingNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void rankingActualizado(String matchId) {
        messaging.convertAndSend(TOPICO, Map.of("matchId", matchId));
    }
}
