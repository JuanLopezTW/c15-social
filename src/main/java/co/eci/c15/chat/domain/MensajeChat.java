package co.eci.c15.chat.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Mensaje de un canal de chat ({@link CanalChat#clave()}). El texto llega ya recortado y con largo valido. */
public record MensajeChat(String id, String canal, String userId, String texto, Instant enviadoEn) {

    public static final int MAX_LONGITUD = 200;

    public MensajeChat {
        Objects.requireNonNull(id);
        Objects.requireNonNull(canal);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(texto);
        Objects.requireNonNull(enviadoEn);
    }

    public static MensajeChat crear(String canal, String userId, String texto, Instant ahora) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("El userId es obligatorio");
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("El mensaje no puede estar vacío");
        String limpio = texto.strip();
        if (limpio.length() > MAX_LONGITUD) {
            throw new IllegalArgumentException("El mensaje no puede superar " + MAX_LONGITUD + " caracteres");
        }
        return new MensajeChat(UUID.randomUUID().toString(), canal, userId, limpio, ahora);
    }
}
