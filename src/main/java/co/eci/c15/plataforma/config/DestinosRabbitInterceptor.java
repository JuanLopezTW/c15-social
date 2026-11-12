package co.eci.c15.plataforma.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

/**
 * RabbitMQ no acepta '/' dentro del nombre de un topic ({@code /topic/chat/global} es invalido), pero el
 * front, la seguridad y los modulos usan rutas con '/'. Este interceptor traduce solo en el borde con el
 * broker: hacia Rabbit {@code /topic/chat/global} pasa a {@code /topic/chat.global} y de vuelta al cliente
 * se restaura. Va despues de la seguridad, que sigue revisando la ruta original.
 */
public class DestinosRabbitInterceptor implements ChannelInterceptor {

    private static final String TOPIC = "/topic/";

    private final boolean haciaRabbit;

    private DestinosRabbitInterceptor(boolean haciaRabbit) {
        this.haciaRabbit = haciaRabbit;
    }

    /** Para lo que va al broker: SUBSCRIBE de los clientes y lo que publica el back. */
    public static DestinosRabbitInterceptor haciaRabbit() {
        return new DestinosRabbitInterceptor(true);
    }

    /** Para lo que el broker le entrega a los clientes. */
    public static DestinosRabbitInterceptor haciaCliente() {
        return new DestinosRabbitInterceptor(false);
    }

    static String aRabbit(String destino) {
        if (destino == null || !destino.startsWith(TOPIC)) return destino;
        return TOPIC + destino.substring(TOPIC.length()).replace('/', '.');
    }

    static String aCliente(String destino) {
        if (destino == null || !destino.startsWith(TOPIC)) return destino;
        return TOPIC + destino.substring(TOPIC.length()).replace('.', '/');
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        String destino = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
        String traducido = haciaRabbit ? aRabbit(destino) : aCliente(destino);
        if (destino == null || destino.equals(traducido)) return message;

        MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
        if (accessor instanceof SimpMessageHeaderAccessor simp && simp.isMutable()) {
            simp.setDestination(traducido);
            return message;
        }
        SimpMessageHeaderAccessor copia = (SimpMessageHeaderAccessor) MessageHeaderAccessor.getMutableAccessor(message);
        copia.setDestination(traducido);
        return MessageBuilder.createMessage(message.getPayload(), copia.getMessageHeaders());
    }
}
