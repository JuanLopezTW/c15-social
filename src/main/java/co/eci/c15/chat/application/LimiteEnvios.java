package co.eci.c15.chat.application;

import co.eci.c15.chat.domain.DemasiadosMensajesException;

import java.time.Duration;
import java.time.Instant;

/** Anti-spam: cada usuario puede enviar pocos mensajes por ventana de tiempo, sumando todos los canales. */
public interface LimiteEnvios {

    int MAX_MENSAJES = 5;
    Duration VENTANA = Duration.ofSeconds(5);

    /** Cuenta el envio o lanza {@link DemasiadosMensajesException} si el usuario ya llego al maximo. */
    void registrar(String userId, Instant ahora);
}
