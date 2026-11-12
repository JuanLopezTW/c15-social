package co.eci.c15.plataforma.eventos;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.common.events.SalaCerradaEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Optional;

/**
 * Los eventos entre servicios que escucha este servicio (contrato: un topic por evento con prefijo, llave =
 * matchId/salaId, valor = JSON del record). Los records son copia de los del monolito, con los mismos campos.
 */
final class TopicosEventos {

    /** Un topic (sin prefijo), la clase en la que se lee y el campo del log que lleva la llave. */
    record Topico(String nombre, Class<?> tipo, String campoLog) {
    }

    static final List<Topico> ESCUCHADOS = List.of(
            new Topico("partida-iniciada", PartidaIniciadaEvent.class, "matchId"),
            new Topico("partida-terminada", MatchFinishedEvent.class, "matchId"),
            new Topico("sala-cerrada", SalaCerradaEvent.class, "salaId"));

    private TopicosEventos() {
    }

    /**
     * Como se lee el JSON, fijo para no depender de la configuracion del servicio: duraciones en ISO-8601
     * ("PT10M") y los campos que no se conocen se ignoran (otro servicio puede agregar campos).
     */
    static ObjectMapper json() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /** Los topics con su prefijo (los usa el {@code @KafkaListener}). */
    static String[] topics(String prefijo) {
        return ESCUCHADOS.stream().map(t -> prefijo + t.nombre()).toArray(String[]::new);
    }

    static Optional<Topico> porNombre(String prefijo, String topicConPrefijo) {
        return ESCUCHADOS.stream().filter(t -> (prefijo + t.nombre()).equals(topicConPrefijo)).findFirst();
    }
}
