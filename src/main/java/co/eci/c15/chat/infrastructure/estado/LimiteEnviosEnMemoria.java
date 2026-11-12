package co.eci.c15.chat.infrastructure.estado;

import co.eci.c15.chat.application.LimiteEnvios;
import co.eci.c15.chat.domain.DemasiadosMensajesException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Ventana deslizante por usuario en memoria (una sola instancia). */
@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class LimiteEnviosEnMemoria implements LimiteEnvios {

    private final Map<String, Deque<Instant>> porUsuario = new ConcurrentHashMap<>();

    @Override
    public void registrar(String userId, Instant ahora) {
        porUsuario.compute(userId, (id, envios) -> {
            Deque<Instant> recientes = envios != null ? envios : new ArrayDeque<>();
            while (!recientes.isEmpty() && !recientes.peekFirst().isAfter(ahora.minus(VENTANA))) {
                recientes.removeFirst();
            }
            if (recientes.size() >= MAX_MENSAJES) throw new DemasiadosMensajesException();
            recientes.addLast(ahora);
            return recientes;
        });
    }
}
