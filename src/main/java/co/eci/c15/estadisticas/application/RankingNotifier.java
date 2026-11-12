package co.eci.c15.estadisticas.application;

/** Avisa a quien este mirando el ranking que cambio, para que lo vuelva a pedir. */
public interface RankingNotifier {

    void rankingActualizado(String matchId);
}
