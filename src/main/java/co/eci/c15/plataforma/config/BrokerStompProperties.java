package co.eci.c15.plataforma.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Broker de los topics STOMP. Apagado (por defecto) usa el broker en memoria de Spring, que solo sirve
 * con una instancia: lo publicado en una no le llega a quien esta conectado a otra. Encendido, cada mensaje pasa
 * por RabbitMQ (plugin STOMP) y cada instancia recibe lo de las demas.
 *
 * @param relay        si se usa RabbitMQ
 * @param host         host del broker
 * @param puerto       puerto STOMP del broker (61613)
 * @param usuario      usuario del broker
 * @param clave        clave del broker
 * @param autoArranque si el relay se conecta al arrancar (los tests lo apagan para no buscar un broker)
 */
@ConfigurationProperties(prefix = "c15.stomp.broker")
public record BrokerStompProperties(boolean relay, String host, int puerto, String usuario, String clave,
                                    Boolean autoArranque) {

    public BrokerStompProperties {
        if (host == null || host.isBlank()) host = "localhost";
        if (puerto <= 0) puerto = 61613;
        if (autoArranque == null) autoArranque = true;
    }
}
