package co.eci.c15.chat.application;

/**
 * Lo que el chat necesita saber de las salas. El chat no depende del modulo salas-equipos:
 * la implementacion vive en el modulo que ensambla ambos (app). Si chat pasa a ser un
 * microservicio, esto se vuelve una llamada HTTP o una vista alimentada por eventos.
 */
public interface SalaEsperaConsulta {

    /** La sala existe y todavia no inicia la partida. */
    boolean enEspera(String salaId);

    boolean contieneJugador(String salaId, String userId);
}
