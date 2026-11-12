package co.eci.c15.estadisticas.infrastructure.persistence;

import co.eci.c15.estadisticas.domain.EstadisticaJugador;
import co.eci.c15.estadisticas.domain.EstadisticasRepository;
import co.eci.c15.estadisticas.domain.PartidaJugada;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EstadisticasRepositoryAdapter implements EstadisticasRepository {

    private final PartidaJugadaJpaRepository jpa;

    public EstadisticasRepositoryAdapter(PartidaJugadaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existePartida(String matchId) {
        return jpa.existsByMatchId(matchId);
    }

    @Override
    public void guardarTodas(List<PartidaJugada> partidas) {
        jpa.saveAll(partidas.stream().map(PartidaJugadaJpa::from).toList());
    }

    @Override
    public List<EstadisticaJugador> ranking(int limite) {
        return jpa.ranking(PageRequest.of(0, limite));
    }

    @Override
    public Optional<EstadisticaJugador> deJugador(String userId) {
        return jpa.totalesDe(userId);
    }

    @Override
    public List<PartidaJugada> ultimasDeJugador(String userId, int limite) {
        return jpa.findByUserIdOrderByJugadaEnDesc(userId, PageRequest.of(0, limite)).stream()
                .map(PartidaJugadaJpa::toDomain)
                .toList();
    }
}
