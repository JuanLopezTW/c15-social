package co.eci.c15.plataforma.observabilidad;

import org.slf4j.MDC;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Campos que acompanan a cada linea de log de una peticion o mensaje, y a las tareas que estos programen. */
final class ContextoLog {

    static final String USER_ID = "userId";
    static final String SALA_ID = "salaId";
    static final String MATCH_ID = "matchId";

    /** Rutas de la API y destinos STOMP; el id se limita a caracteres seguros porque viene del cliente. */
    private static final Pattern RECURSO = Pattern.compile(
            "^/(?:api/(?:chat/)?|dev/|topic/(?:chat/)?|app/)(salas|partidas)/([A-Za-z0-9-]{1,64})(?:/.*)?$");

    private ContextoLog() {
    }

    static void ponerRecurso(String rutaODestino) {
        if (rutaODestino == null) return;
        Matcher recurso = RECURSO.matcher(rutaODestino);
        if (recurso.matches()) {
            MDC.put("salas".equals(recurso.group(1)) ? SALA_ID : MATCH_ID, recurso.group(2));
        }
    }

    static void limpiar() {
        MDC.remove(USER_ID);
        MDC.remove(SALA_ID);
        MDC.remove(MATCH_ID);
    }
}
