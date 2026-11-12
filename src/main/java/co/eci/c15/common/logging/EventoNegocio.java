package co.eci.c15.common.logging;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Algo que paso en el juego, con nombre fijo para poder buscarlo: {@code partida.iniciada}, {@code sala.cerrada}...
 * Cada campo queda como dato consultable y en el mensaje. Si ya esta en el contexto de log (MDC) con el mismo
 * valor no se repite, porque duplicaria la clave en el JSON. Los valores se limpian: pueden venir del cliente.
 * Nunca pasar aqui tokens, correos ni el texto del chat.
 */
public final class EventoNegocio {

    private static final int MAX_VALOR = 200;

    private final Logger log;
    private final String nombre;
    private final Map<String, Object> campos = new LinkedHashMap<>();

    private EventoNegocio(Logger log, String nombre) {
        this.log = log;
        this.nombre = nombre;
    }

    public static EventoNegocio de(Logger log, String nombre) {
        return new EventoNegocio(log, nombre);
    }

    public EventoNegocio con(String campo, Object valor) {
        campos.put(campo, valor instanceof Number ? valor : limpio(valor));
        return this;
    }

    public void info() {
        emitir(log.atInfo());
    }

    public void warn() {
        emitir(log.atWarn());
    }

    private void emitir(LoggingEventBuilder evento) {
        StringBuilder mensaje = new StringBuilder(nombre);
        evento.addKeyValue("evento", nombre);
        campos.forEach((campo, valor) -> {
            mensaje.append(' ').append(campo).append('=').append(valor);
            String enContexto = MDC.get(campo);
            if (enContexto == null) evento.addKeyValue(campo, valor);
            else if (!enContexto.equals(String.valueOf(valor))) evento.addKeyValue("evento." + campo, valor);
        });
        evento.log(mensaje.toString());
    }

    private static String limpio(Object valor) {
        String texto = String.valueOf(valor).replaceAll("\\p{Cntrl}", "_");
        return texto.length() > MAX_VALOR ? texto.substring(0, MAX_VALOR) : texto;
    }
}
