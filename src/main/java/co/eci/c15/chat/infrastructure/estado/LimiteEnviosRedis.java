package co.eci.c15.chat.infrastructure.estado;

import co.eci.c15.chat.application.LimiteEnvios;
import co.eci.c15.chat.domain.DemasiadosMensajesException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * La misma ventana deslizante, pero compartida: un set ordenado por usuario con la hora de cada envio. El
 * script corre entero dentro de Redis, asi que dos instancias no pueden colar un sexto mensaje a la vez.
 * La clave caduca con la ventana (5 s).
 */
@Component
// Nunca convive con el adaptador en memoria (lo decide c15.estado.redis); @Primary solo le aclara
// al analisis estatico cual usar, porque no evalua el interruptor.
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "true")
@Primary
public class LimiteEnviosRedis implements LimiteEnvios {

    static final String PREFIJO = "c15:chat:limite:";

    private static final RedisScript<Long> VENTANA_DESLIZANTE = RedisScript.of("""
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', tonumber(ARGV[1]) - tonumber(ARGV[2]))
            if redis.call('ZCARD', KEYS[1]) >= tonumber(ARGV[3]) then return 0 end
            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    public LimiteEnviosRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void registrar(String userId, Instant ahora) {
        Long permitido = redis.execute(VENTANA_DESLIZANTE, List.of(PREFIJO + userId),
                String.valueOf(ahora.toEpochMilli()), String.valueOf(VENTANA.toMillis()),
                String.valueOf(MAX_MENSAJES), UUID.randomUUID().toString());
        if (permitido == null || permitido == 0) throw new DemasiadosMensajesException();
    }
}
