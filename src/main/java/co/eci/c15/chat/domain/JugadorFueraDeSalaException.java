package co.eci.c15.chat.domain;

public class JugadorFueraDeSalaException extends RuntimeException {
    public JugadorFueraDeSalaException(String salaId, String userId) {
        super("El jugador " + userId + " no está en la sala " + salaId);
    }
}
