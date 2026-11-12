package co.eci.c15.chat.domain;

public class DemasiadosMensajesException extends RuntimeException {
    public DemasiadosMensajesException() {
        super("Vas muy rápido, espera un momento");
    }
}
