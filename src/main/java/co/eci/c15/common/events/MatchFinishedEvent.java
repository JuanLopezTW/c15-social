package co.eci.c15.common.events;

import java.util.List;
import java.util.Map;

/**
 * The match ended (HU-67.6). {@code winnerTeamId} is null on a draw; {@code scores} is the
 * final score of every team. {@code reason}: TIME_UP or ALL_PUZZLES_SOLVED.
 *
 * <p>{@code equipos} (equipoId → userIds, como en {@link PartidaIniciadaEvent}) viaja aqui para que
 * quien lo escuche no tenga que recordar los equipos desde el inicio: el fin lo puede atender otra
 * instancia u otro servicio.
 */
public record MatchFinishedEvent(String matchId, String reason, String winnerTeamId, Map<String, Integer> scores,
                                 Map<String, List<String>> equipos) {
}
