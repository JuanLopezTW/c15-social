package co.eci.c15.chat.domain;

import java.util.List;

/** Historial de cada canal de chat. Solo guarda los ultimos mensajes. */
public interface ChatRepository {
    void guardar(MensajeChat mensaje);

    /** Mensajes del canal, del mas viejo al mas nuevo. */
    List<MensajeChat> historial(String canal);

    void borrar(String canal);
}
