package co.eci.c15.chat.infrastructure.web;

import co.eci.c15.chat.application.MensajeChatDto;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.mockito.Mockito.*;

class StompChatNotifierTest {

    @Test
    void publicaElMensajeEnElTopicDeSuCanal() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        MensajeChatDto mensaje = new MensajeChatDto("m1", "salas/s1", "ana", "hola", Instant.EPOCH);

        new StompChatNotifier(messaging).mensajeNuevo(mensaje);

        verify(messaging).convertAndSend("/topic/chat/salas/s1", mensaje);
    }
}
