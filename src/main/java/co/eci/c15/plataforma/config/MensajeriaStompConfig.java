package co.eci.c15.plataforma.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpSubscriptionMatcher;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.List;
import java.util.Set;

/**
 * Este servicio no tiene WebSocket: los jugadores se conectan al broker (c15-broker). Aqui solo se publica
 * (SimpMessagingTemplate) en los topics del chat y del ranking, y con {@code c15.stomp.broker.relay=true}
 * eso sale por RabbitMQ y le llega a quien este suscrito en el broker. Con el relay apagado (pruebas) se usa
 * el broker en memoria, que no tiene a nadie conectado.
 *
 * Topics de este servicio:
 *   /topic/chat/global, /topic/chat/salas/{id}, /topic/chat/partidas/{id}, /topic/chat/partidas/{id}/equipos/{eq}
 *   /topic/estadisticas/ranking
 */
@Configuration
@EnableConfigurationProperties(BrokerStompProperties.class)
public class MensajeriaStompConfig extends AbstractMessageBrokerConfiguration {

    private final BrokerStompProperties broker;
    private final ObjectMapper json;

    public MensajeriaStompConfig(BrokerStompProperties broker, ObjectMapper json) {
        this.broker = broker;
        this.json = json;
    }

    /** El JSON de los mensajes sale igual que el de la API REST (fechas en ISO-8601), con el ObjectMapper de Spring Boot. */
    @Override
    protected boolean configureMessageConverters(List<MessageConverter> converters) {
        converters.add(new MappingJackson2MessageConverter(json));
        return false;
    }

    @Override
    protected void configureMessageBroker(MessageBrokerRegistry registry) {
        if (broker.relay()) {
            registry.enableStompBrokerRelay("/topic")
                    .setRelayHost(broker.host())
                    .setRelayPort(broker.puerto())
                    .setClientLogin(broker.usuario())
                    .setClientPasscode(broker.clave())
                    .setSystemLogin(broker.usuario())
                    .setSystemPasscode(broker.clave())
                    // Si no, Rabbit toma el "host" del cliente como virtual host
                    .setVirtualHost("/")
                    .setAutoStartup(broker.autoArranque());
            // Rabbit no acepta "/" en los topics: /topic/a/b viaja como /topic/a.b
            registry.configureBrokerChannel().interceptors(DestinosRabbitInterceptor.haciaRabbit());
        } else {
            registry.enableSimpleBroker("/topic");
        }
    }

    /** Nadie se conecta a este servicio: no hay usuarios que registrar. */
    @Override
    protected SimpUserRegistry createLocalUserRegistry(Integer order) {
        return new SimpUserRegistry() {
            @Override
            public SimpUser getUser(String userName) {
                return null;
            }

            @Override
            public Set<SimpUser> getUsers() {
                return Set.of();
            }

            @Override
            public int getUserCount() {
                return 0;
            }

            @Override
            public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) {
                return Set.of();
            }
        };
    }
}
