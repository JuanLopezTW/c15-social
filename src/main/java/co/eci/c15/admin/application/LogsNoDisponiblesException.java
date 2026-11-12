package co.eci.c15.admin.application;

/** Los logs centralizados no estan configurados (fuera de Azure) o no respondieron. */
public class LogsNoDisponiblesException extends RuntimeException {

    public LogsNoDisponiblesException(String mensaje) {
        super(mensaje);
    }

    public LogsNoDisponiblesException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
