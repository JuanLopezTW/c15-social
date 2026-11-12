package co.eci.c15.chat.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Quien es de que equipo en cada partida en curso (llega con PartidaIniciadaEvent). */
public interface EquiposDePartida {

    void guardar(String matchId, Map<String, List<String>> equipos);

    /** equipoId -> jugadores; vacio si la partida no existe o ya termino. */
    Optional<Map<String, List<String>>> de(String matchId);

    void borrar(String matchId);
}
