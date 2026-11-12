package co.eci.c15.chat.infrastructure.estado;

import co.eci.c15.chat.application.EquiposDePartida;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "c15.estado.redis", havingValue = "false", matchIfMissing = true)
public class EquiposDePartidaEnMemoria implements EquiposDePartida {

    private final Map<String, Map<String, List<String>>> partidas = new ConcurrentHashMap<>();

    @Override
    public void guardar(String matchId, Map<String, List<String>> equipos) {
        partidas.put(matchId, Map.copyOf(equipos));
    }

    @Override
    public Optional<Map<String, List<String>>> de(String matchId) {
        return Optional.ofNullable(partidas.get(matchId));
    }

    @Override
    public void borrar(String matchId) {
        partidas.remove(matchId);
    }
}
