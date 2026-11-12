package co.eci.c15.chat.infrastructure.web;

import co.eci.c15.chat.application.ChatNotifier;
import co.eci.c15.chat.application.MensajeChatDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Publica cada mensaje en /topic/chat/{canal}. */
@Component
public class StompChatNotifier implements ChatNotifier {

    private final SimpMessagingTemplate messaging;

    public StompChatNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void mensajeNuevo(MensajeChatDto mensaje) {
        messaging.convertAndSend("/topic/chat/" + mensaje.canal(), mensaje);
    }
}
