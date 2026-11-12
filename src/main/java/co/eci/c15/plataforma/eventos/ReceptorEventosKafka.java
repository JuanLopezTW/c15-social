package co.eci.c15.plataforma.eventos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.UncheckedIOException;

/**
 * Recibe los eventos entre servicios de Kafka y los reparte a los listeners de este servicio con
 * {@link ApplicationEventPublisher}: los modulos los escuchan con {@code @EventListener}, como en el monolito.
 * Las replicas de este servicio estan en el mismo grupo: cada evento lo atiende una sola. Kafka puede
 * entregar un evento repetido: los listeners son idempotentes.
 */
@Component
@ConditionalOnProperty(name = "c15.eventos.kafka", havingValue = "true")
public class ReceptorEventosKafka {

    private final ApplicationEventPublisher eventos;
    private final ObjectMapper json = TopicosEventos.json();
    private final String prefijo;

    public ReceptorEventosKafka(ApplicationEventPublisher eventos, @Value("${c15.eventos.prefijo:c15.}") String prefijo) {
        this.eventos = eventos;
        this.prefijo = prefijo;
    }

    @KafkaListener(topics = "#{T(co.eci.c15.plataforma.eventos.KafkaEventosConfig).topics('${c15.eventos.prefijo:c15.}')}",
            groupId = "${c15.eventos.grupo:c15-social}")
    void recibir(ConsumerRecord<String, String> registro) {
        TopicosEventos.Topico topico = TopicosEventos.porNombre(prefijo, registro.topic()).orElse(null);
        if (topico == null) return;
        MDC.put(topico.campoLog(), registro.key());
        try {
            // Si un listener falla, el error sube y Kafka lo reintenta (ver KafkaEventosConfig)
            eventos.publishEvent(leer(registro.value(), topico.tipo()));
        } finally {
            MDC.remove(topico.campoLog());
        }
    }

    private Object leer(String valor, Class<?> tipo) {
        try {
            return json.readValue(valor, tipo);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
