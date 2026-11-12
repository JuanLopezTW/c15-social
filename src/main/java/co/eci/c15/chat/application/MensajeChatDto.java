package co.eci.c15.chat.application;

import co.eci.c15.chat.domain.MensajeChat;

import java.time.Instant;

public record MensajeChatDto(String id, String canal, String userId, String texto, Instant enviadoEn) {
    public static MensajeChatDto from(MensajeChat m) {
        return new MensajeChatDto(m.id(), m.canal(), m.userId(), m.texto(), m.enviadoEn());
    }
}
