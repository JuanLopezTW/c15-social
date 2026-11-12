package co.eci.c15.estadisticas.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.Mockito.*;

class StompRankingNotifierTest {

    @Test
    void publicaElAvisoEnElTopicoDelRanking() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);

        new StompRankingNotifier(messaging).rankingActualizado("m1");

        verify(messaging).convertAndSend("/topic/estadisticas/ranking", Map.of("matchId", "m1"));
    }
}
