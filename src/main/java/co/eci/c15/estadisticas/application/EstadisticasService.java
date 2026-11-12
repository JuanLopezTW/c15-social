package co.eci.c15.estadisticas.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.estadisticas.domain.EstadisticaJugador;
import co.eci.c15.estadisticas.domain.EstadisticasRepository;
import co.eci.c15.estadisticas.domain.PartidaJugada;
import co.eci.c15.estadisticas.domain.ResultadoPartida;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Registra el resultado de cada jugador cuando termina una partida y arma el ranking. No guarda nada
 * en memoria: el evento de fin trae los equipos, asi da igual que instancia (o servicio) lo atienda.
 */
@Service
public class EstadisticasService {

    private static final Logger log = LoggerFactory.getLogger(EstadisticasService.class);

    static final int MAX_RANKING = 50;
    static final int ULTIMAS_PARTIDAS = 5;

    private final EstadisticasRepository repositorio;
    private final Clock clock;

    @Autowired
    public EstadisticasService(EstadisticasRepository repositorio) {
        this(repositorio, Clock.systemUTC());
    }

    EstadisticasService(EstadisticasRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    /** Devuelve si la partida quedo registrada (false si no trae equipos o ya estaba). */
    @Transactional
    public boolean partidaTerminada(MatchFinishedEvent evento) {
        Map<String, List<String>> equipos = evento.equipos();
        if (equipos == null || equipos.isEmpty() || repositorio.existePartida(evento.matchId())) return false;

        Instant ahora = clock.instant();
        int mejorPuntaje = evento.scores().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<PartidaJugada> partidas = new ArrayList<>();
        equipos.forEach((equipoId, jugadores) -> {
            int puntaje = evento.scores().getOrDefault(equipoId, 0);
            ResultadoPartida resultado = resultadoDe(equipoId, puntaje, mejorPuntaje, evento.winnerTeamId());
            jugadores.forEach(userId ->
                    partidas.add(new PartidaJugada(evento.matchId(), userId, equipoId, resultado, puntaje, ahora)));
        });
        repositorio.guardarTodas(partidas);
        EventoNegocio.de(log, "estadisticas.partida_registrada").con("matchId", evento.matchId())
                .con("jugadores", partidas.size()).info();
        return true;
    }

    public List<JugadorRankingDto> ranking(int limite) {
        List<EstadisticaJugador> mejores = repositorio.ranking(Math.max(1, Math.min(limite, MAX_RANKING)));
        List<JugadorRankingDto> ranking = new ArrayList<>();
        for (int i = 0; i < mejores.size(); i++) ranking.add(JugadorRankingDto.from(i + 1, mejores.get(i)));
        return Collections.unmodifiableList(ranking);
    }

    public EstadisticasJugadorDto deJugador(String userId) {
        EstadisticaJugador totales = repositorio.deJugador(userId).orElse(EstadisticaJugador.sinPartidas(userId));
        return EstadisticasJugadorDto.from(totales, repositorio.ultimasDeJugador(userId, ULTIMAS_PARTIDAS));
    }

    /** Sin ganador, empatan los que llegaron al mejor puntaje; los demas pierden. */
    private static ResultadoPartida resultadoDe(String equipoId, int puntaje, int mejorPuntaje, String ganador) {
        if (ganador != null) return ganador.equals(equipoId) ? ResultadoPartida.VICTORIA : ResultadoPartida.DERROTA;
        return puntaje == mejorPuntaje ? ResultadoPartida.EMPATE : ResultadoPartida.DERROTA;
    }
}
