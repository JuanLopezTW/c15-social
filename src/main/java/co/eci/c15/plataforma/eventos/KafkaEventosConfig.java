package co.eci.c15.plataforma.eventos;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.io.UncheckedIOException;

/** Topics y reintentos de los eventos entre servicios (solo con {@code c15.eventos.kafka=true}). */
@Configuration
@ConditionalOnProperty(name = "c15.eventos.kafka", havingValue = "true")
public class KafkaEventosConfig {

    /** Las mismas 3 particiones en todos los servicios: el primero que arranque crea el topic. */
    private static final int PARTICIONES = 3;

    /** Los topics escuchados con su prefijo (lo usa el {@code @KafkaListener}). */
    public static String[] topics(String prefijo) {
        return TopicosEventos.topics(prefijo);
    }

    /** Crea los topics que escucha si aun no existen (spring.kafka.admin.auto-create; en Event Hubs se crean a mano). */
    @Bean
    KafkaAdmin.NewTopics topicosEventos(@Value("${c15.eventos.prefijo:c15.}") String prefijo) {
        return new KafkaAdmin.NewTopics(TopicosEventos.ESCUCHADOS.stream()
                .map(t -> TopicBuilder.name(prefijo + t.nombre()).partitions(PARTICIONES).replicas(1).build())
                .toArray(NewTopic[]::new));
    }

    /**
     * Si un listener falla, el evento se reintenta 3 veces con 1 s entre intentos y despues se deja en el log
     * (no frena a los siguientes). Un JSON que no se puede leer no se reintenta.
     */
    @Bean
    CommonErrorHandler erroresEventos() {
        DefaultErrorHandler errores = new DefaultErrorHandler(new FixedBackOff(1000L, 3L));
        errores.addNotRetryableExceptions(UncheckedIOException.class);
        return errores;
    }
}
