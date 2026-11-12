package co.eci.c15.chat.infrastructure.events;

import co.eci.c15.chat.application.ChatService;
import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.common.events.SalaCerradaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Al arrancar la partida se cierra el chat de la sala (matchId = salaId) y se abren los de la
 * partida, que se olvidan al terminar. Si la sala desaparece antes, su chat tambien se cierra.
 */
@Component
public class ChatPartidaListener {

    private final ChatService chat;

    public ChatPartidaListener(ChatService chat) {
        this.chat = chat;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        chat.partidaIniciada(event.matchId(), event.equipos());
    }

    @EventListener
    public void onPartidaTerminada(MatchFinishedEvent event) {
        chat.partidaTerminada(event.matchId());
    }

    @EventListener
    public void onSalaCerrada(SalaCerradaEvent event) {
        chat.cerrarSala(event.salaId());
    }
}
