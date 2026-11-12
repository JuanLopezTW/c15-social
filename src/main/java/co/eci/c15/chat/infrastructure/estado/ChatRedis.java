package co.eci.c15.chat.infrastructure.estado;

import co.eci.c15.chat.domain.ChatRepository;
import co.eci.c15.chat.domain.MensajeChat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

import java.util.List;

import static co.eci.c15.chat.infrastructure.persistence.ChatEnMemoria.MAX_MENSAJES;

/** Historial de cada canal como lista de Redis: solo los ultimos mensajes y con caducidad. */
@Repository
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class ChatRedis implements ChatRepository {

    static final String PREFIJO = "c15:chat:";

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final CaducidadChat caducidad;

    public ChatRedis(StringRedisTemplate redis, ObjectMapper json, CaducidadChat caducidad) {
        this.redis = redis;
        this.json = json;
        this.caducidad = caducidad;
    }

    @Override
    public void guardar(MensajeChat mensaje) {
        String clave = PREFIJO + mensaje.canal();
        redis.opsForList().rightPush(clave, aJson(mensaje));
        redis.opsForList().trim(clave, -MAX_MENSAJES, -1);
        redis.expire(clave, caducidad.deCanal(mensaje.canal()));
    }

    @Override
    public List<MensajeChat> historial(String canal) {
        List<String> guardados = redis.opsForList().range(PREFIJO + canal, 0, -1);
        if (guardados == null) return List.of();
        return guardados.stream().map(this::deJson).toList();
    }

    @Override
    public void borrar(String canal) {
        redis.delete(PREFIJO + canal);
    }

    private String aJson(MensajeChat mensaje) {
        try {
            return json.writeValueAsString(mensaje);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar el mensaje", e);
        }
    }

    private MensajeChat deJson(String guardado) {
        try {
            return json.readValue(guardado, MensajeChat.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Mensaje de chat corrupto en Redis", e);
        }
    }
}
