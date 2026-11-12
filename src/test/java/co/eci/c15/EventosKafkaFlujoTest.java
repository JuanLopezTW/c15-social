package co.eci.c15;

import co.eci.c15.chat.domain.ChatRepository;
import co.eci.c15.chat.application.EquiposDePartida;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los eventos llegan por Kafka con el JSON del contrato (como lo publican salas y gameplay), no como clases Java:
 * asi se prueba que este servicio los lee igual, aunque traigan campos que no conoce.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class EventosKafkaFlujoTest {

    @SuppressWarnings("resource")
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.1.0");

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry propiedades) {
        if (!KAFKA.isRunning()) KAFKA.start();
        propiedades.add("c15.eventos.kafka", () -> "true");
        propiedades.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        // Los topics son nuevos: se leen desde el principio
        propiedades.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private EquiposDePartida equipos;

    @Autowired
    private ChatRepository historial;

    @Autowired
    private MockMvc mvc;

    @Test
    void laPartidaIniciadaAbreSusChatsYLaTerminadaSumaAlRanking() throws Exception {
        kafka.send("c15.partida-iniciada", "k-m1",
                "{\"matchId\":\"k-m1\",\"equipos\":{\"k-e1\":[\"k-ana\"],\"k-e2\":[\"k-beto\"]},\"duracion\":\"PT10M\",\"campoNuevo\":1}");

        await().atMost(Duration.ofSeconds(20)).until(() -> equipos.de("k-m1").isPresent());

        kafka.send("c15.partida-terminada", "k-m1",
                "{\"matchId\":\"k-m1\",\"reason\":\"TIME_UP\",\"winnerTeamId\":\"k-e1\",\"scores\":{\"k-e1\":2,\"k-e2\":1},"
                        + "\"equipos\":{\"k-e1\":[\"k-ana\"],\"k-e2\":[\"k-beto\"]}}");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> mvc.perform(get("/api/estadisticas/jugadores/k-ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.victorias").value(1)));
        assertTrue(equipos.de("k-m1").isEmpty(), "al terminar se olvidan los equipos del chat");
    }

    @Test
    void unJsonRotoNoFrenaALosSiguientes() {
        kafka.send("c15.sala-cerrada", "k-s1", "esto no es json");
        historial.guardar(co.eci.c15.chat.domain.MensajeChat.crear("salas/k-s2", "k-ana", "hola", java.time.Instant.now()));
        kafka.send("c15.sala-cerrada", "k-s2", "{\"salaId\":\"k-s2\"}");

        await().atMost(Duration.ofSeconds(20)).until(() -> historial.historial("salas/k-s2").isEmpty());
    }
}
