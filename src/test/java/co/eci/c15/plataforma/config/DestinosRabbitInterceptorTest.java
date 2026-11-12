package co.eci.c15.plataforma.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;

class DestinosRabbitInterceptorTest {

    private static String destino(Message<?> m) {
        return SimpMessageHeaderAccessor.getDestination(m.getHeaders());
    }

    @Test
    void haciaRabbitLasBarrasDelTopicPasanAPuntos() {
        assertEquals("/topic/partidas.m1.equipos.e1.posiciones",
                DestinosRabbitInterceptor.aRabbit("/topic/partidas/m1/equipos/e1/posiciones"));
        assertEquals("/topic/salas", DestinosRabbitInterceptor.aRabbit("/topic/salas"));
    }

    @Test
    void deVueltaAlClienteSeRestauranLasBarras() {
        assertEquals("/topic/chat/partidas/m1", DestinosRabbitInterceptor.aCliente("/topic/chat.partidas.m1"));
    }

    @Test
    void loQueNoEsUnTopicNoSeToca() {
        assertEquals("/app/salas/s1/sesion", DestinosRabbitInterceptor.aRabbit("/app/salas/s1/sesion"));
        assertNull(DestinosRabbitInterceptor.aCliente(null));
    }

    @Test
    void elSubscribeDelClienteSeTraduceEnElMismoMensaje() {
        StompHeaderAccessor stomp = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        stomp.setDestination("/topic/chat/global");
        stomp.setLeaveMutable(true);
        Message<byte[]> subscribe = MessageBuilder.createMessage(new byte[0], stomp.getMessageHeaders());

        Message<?> salida = DestinosRabbitInterceptor.haciaRabbit().preSend(subscribe, null);

        assertSame(subscribe, salida);
        assertEquals("/topic/chat.global", destino(salida));
    }

    @Test
    void unMensajeYaCerradoSeCopiaConElDestinoTraducido() {
        SimpMessageHeaderAccessor simp = SimpMessageHeaderAccessor.create();
        simp.setDestination("/topic/chat.global");
        Message<String> cerrado = MessageBuilder.createMessage("hola", simp.getMessageHeaders());

        Message<?> salida = DestinosRabbitInterceptor.haciaCliente().preSend(cerrado, null);

        assertEquals("/topic/chat/global", destino(salida));
        assertEquals("hola", salida.getPayload());
    }

    @Test
    void sinCambiosDevuelveElMismoMensaje() {
        Message<String> sinDestino = MessageBuilder.withPayload("x").build();

        assertSame(sinDestino, DestinosRabbitInterceptor.haciaRabbit().preSend(sinDestino, null));
    }
}
