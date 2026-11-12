package co.eci.c15.chat.domain;

/** Donde se habla: el menu (global), la sala de espera, toda la partida o un equipo de la partida. */
public record CanalChat(String clave) {

    public static final CanalChat GLOBAL = new CanalChat("global");

    public static CanalChat sala(String salaId) {
        return new CanalChat("salas/" + salaId);
    }

    public static CanalChat partida(String matchId) {
        return new CanalChat("partidas/" + matchId);
    }

    public static CanalChat equipo(String matchId, String equipoId) {
        return new CanalChat("partidas/" + matchId + "/equipos/" + equipoId);
    }
}
