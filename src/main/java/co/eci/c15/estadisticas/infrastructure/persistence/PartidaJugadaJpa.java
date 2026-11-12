package co.eci.c15.estadisticas.infrastructure.persistence;

import co.eci.c15.estadisticas.domain.PartidaJugada;
import co.eci.c15.estadisticas.domain.ResultadoPartida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** El resultado va como texto y sin @Enumerated: Hibernate le crea un CHECK que ddl-auto update no cambia. */
@Entity
@Table(name = "partidas_jugadas", indexes = {
        @Index(name = "idx_partidas_jugadas_user", columnList = "userId"),
        @Index(name = "idx_partidas_jugadas_match", columnList = "matchId")
})
class PartidaJugadaJpa {

    /** matchId:userId, para que un jugador cuente una sola vez por partida. */
    @Id
    private String id;

    @Column(nullable = false)
    private String matchId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String equipoId;

    @Column(nullable = false, length = 10)
    private String resultado;

    @Column(nullable = false)
    private int puntajeEquipo;

    @Column(nullable = false)
    private Instant jugadaEn;

    protected PartidaJugadaJpa() {}

    static PartidaJugadaJpa from(PartidaJugada p) {
        PartidaJugadaJpa jpa = new PartidaJugadaJpa();
        jpa.id = p.matchId() + ":" + p.userId();
        jpa.matchId = p.matchId();
        jpa.userId = p.userId();
        jpa.equipoId = p.equipoId();
        jpa.resultado = p.resultado().name();
        jpa.puntajeEquipo = p.puntajeEquipo();
        jpa.jugadaEn = p.jugadaEn();
        return jpa;
    }

    PartidaJugada toDomain() {
        return new PartidaJugada(matchId, userId, equipoId, ResultadoPartida.valueOf(resultado), puntajeEquipo, jugadaEn);
    }
}
