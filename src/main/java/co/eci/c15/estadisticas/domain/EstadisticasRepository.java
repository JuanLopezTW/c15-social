package co.eci.c15.estadisticas.domain;

import java.util.List;
import java.util.Optional;

public interface EstadisticasRepository {
    boolean existePartida(String matchId);

    void guardarTodas(List<PartidaJugada> partidas);

    /** Los mejores: mas puntos, luego mas victorias. */
    List<EstadisticaJugador> ranking(int limite);

    Optional<EstadisticaJugador> deJugador(String userId);

    /** Las ultimas partidas del jugador, de la mas nueva a la mas vieja. */
    List<PartidaJugada> ultimasDeJugador(String userId, int limite);
}
