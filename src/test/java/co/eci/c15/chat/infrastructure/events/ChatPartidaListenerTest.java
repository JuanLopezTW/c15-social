package co.eci.c15.chat.infrastructure.events;

import co.eci.c15.chat.application.ChatService;
import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.common.events.SalaCerradaEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class ChatPartidaListenerTest {

    private final ChatService chat = mock(ChatService.class);
    private final ChatPartidaListener listener = new ChatPartidaListener(chat);

    @Test
    void alIniciarLaPartidaSeAbrenSusCanalesConLosEquipos() {
        Map<String, List<String>> equipos = Map.of("e1", List.of("ana"));

        listener.onPartidaIniciada(new PartidaIniciadaEvent("s1", equipos));

        verify(chat).partidaIniciada("s1", equipos);
    }

    @Test
    void alTerminarLaPartidaSeOlvidanSusCanales() {
        listener.onPartidaTerminada(new MatchFinishedEvent("s1", "TIME_UP", null, Map.of(), Map.of()));

        verify(chat).partidaTerminada("s1");
    }

    @Test
    void siLaSalaDesapareceSeCierraSuChat() {
        listener.onSalaCerrada(new SalaCerradaEvent("s1"));

        verify(chat).cerrarSala("s1");
    }
}
