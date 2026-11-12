package co.eci.c15.chat.infrastructure.persistence;

import co.eci.c15.chat.domain.ChatRepository;
import co.eci.c15.chat.domain.MensajeChat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** El chat es efimero: vive en memoria y solo guarda los ultimos mensajes de cada canal. */
@Repository
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class ChatEnMemoria implements ChatRepository {

    public static final int MAX_MENSAJES = 50;

    private final Map<String, Deque<MensajeChat>> porCanal = new ConcurrentHashMap<>();

    @Override
    public void guardar(MensajeChat mensaje) {
        porCanal.compute(mensaje.canal(), (id, cola) -> {
            Deque<MensajeChat> c = cola != null ? cola : new ArrayDeque<>();
            synchronized (c) {
                c.addLast(mensaje);
                while (c.size() > MAX_MENSAJES) c.removeFirst();
            }
            return c;
        });
    }

    @Override
    public List<MensajeChat> historial(String canal) {
        Deque<MensajeChat> cola = porCanal.get(canal);
        if (cola == null) return List.of();
        synchronized (cola) {
            return List.copyOf(cola);
        }
    }

    @Override
    public void borrar(String canal) {
        porCanal.remove(canal);
    }
}
