package co.eci.c15.estadisticas.infrastructure.persistence;

import co.eci.c15.estadisticas.domain.EstadisticaJugador;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface PartidaJugadaJpaRepository extends JpaRepository<PartidaJugadaJpa, String> {

    String TOTALES = """
            select new co.eci.c15.estadisticas.domain.EstadisticaJugador(p.userId, count(p),
                sum(case when p.resultado = 'VICTORIA' then 1L else 0L end),
                sum(case when p.resultado = 'EMPATE' then 1L else 0L end),
                sum(case when p.resultado = 'DERROTA' then 1L else 0L end))
            from PartidaJugadaJpa p
            """;

    boolean existsByMatchId(String matchId);

    @Query(TOTALES + """
            group by p.userId
            order by 3 * sum(case when p.resultado = 'VICTORIA' then 1L else 0L end)
                       + sum(case when p.resultado = 'EMPATE' then 1L else 0L end) desc,
                     sum(case when p.resultado = 'VICTORIA' then 1L else 0L end) desc,
                     p.userId asc
            """)
    List<EstadisticaJugador> ranking(Pageable pagina);

    @Query(TOTALES + "where p.userId = :userId group by p.userId")
    Optional<EstadisticaJugador> totalesDe(@Param("userId") String userId);

    List<PartidaJugadaJpa> findByUserIdOrderByJugadaEnDesc(String userId, Pageable pagina);
}
