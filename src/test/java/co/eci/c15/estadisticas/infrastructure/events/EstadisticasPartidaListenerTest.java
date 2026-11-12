package co.eci.c15.estadisticas.infrastructure.events;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.estadisticas.application.EstadisticasService;
import co.eci.c15.estadisticas.application.RankingNotifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class EstadisticasPartidaListenerTest {

    private final EstadisticasService estadisticas = mock(EstadisticasService.class);
    private final RankingNotifier notifier = mock(RankingNotifier.class);
    private final EstadisticasPartidaListener listener = new EstadisticasPartidaListener(estadisticas, notifier);
    private final MatchFinishedEvent fin = new MatchFinishedEvent("m1", "TIME_UP", null, Map.of(), Map.of());

    @Test
    void alTerminarRegistraElResultadoYAvisaQueCambioElRanking() {
        when(estadisticas.partidaTerminada(fin)).thenReturn(true);

        listener.onPartidaTerminada(fin);

        verify(estadisticas).partidaTerminada(fin);
        verify(notifier).rankingActualizado("m1");
    }

    @Test
    void siLaPartidaNoSeRegistroNoAvisa() {
        when(estadisticas.partidaTerminada(fin)).thenReturn(false);

        listener.onPartidaTerminada(fin);

        verifyNoInteractions(notifier);
    }

    @Test
    void unFalloAlGuardarNoLeDañaElFinDePartidaALosDemasModulos() {
        doThrow(new IllegalStateException("sin base de datos")).when(estadisticas).partidaTerminada(fin);

        assertDoesNotThrow(() -> listener.onPartidaTerminada(fin));
        verifyNoInteractions(notifier);
    }

    @Test
    void unFalloAlAvisarTampocoSale() {
        when(estadisticas.partidaTerminada(fin)).thenReturn(true);
        doThrow(new IllegalStateException("broker caido")).when(notifier).rankingActualizado("m1");

        assertDoesNotThrow(() -> listener.onPartidaTerminada(fin));
    }
}
