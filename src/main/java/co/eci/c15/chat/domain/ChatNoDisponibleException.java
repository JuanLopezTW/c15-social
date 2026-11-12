package co.eci.c15.chat.domain;

/** La sala ya no esta en espera, o la partida no existe o ya termino. */
public class ChatNoDisponibleException extends RuntimeException {
    public ChatNoDisponibleException(String id) {
        super("Este chat ya no está disponible: " + id);
    }
}
