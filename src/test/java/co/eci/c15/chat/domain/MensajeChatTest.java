package co.eci.c15.chat.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MensajeChatTest {

    private static final Instant AHORA = Instant.parse("2026-10-13T15:00:00Z");

    @Test
    void creaElMensajeConElTextoRecortado() {
        MensajeChat m = MensajeChat.crear("s1", "ana", "  hola equipo  ", AHORA);

        assertNotNull(m.id());
        assertEquals("s1", m.canal());
        assertEquals("ana", m.userId());
        assertEquals("hola equipo", m.texto());
        assertEquals(AHORA, m.enviadoEn());
    }

    @Test
    void cadaMensajeTieneSuPropioId() {
        assertNotEquals(MensajeChat.crear("s1", "ana", "a", AHORA).id(), MensajeChat.crear("s1", "ana", "a", AHORA).id());
    }

    @Test
    void rechazaTextoVacioONulo() {
        assertThrows(IllegalArgumentException.class, () -> MensajeChat.crear("s1", "ana", "   ", AHORA));
        assertThrows(IllegalArgumentException.class, () -> MensajeChat.crear("s1", "ana", null, AHORA));
    }

    @Test
    void rechazaUsuarioVacioONulo() {
        assertThrows(IllegalArgumentException.class, () -> MensajeChat.crear("s1", " ", "hola", AHORA));
        assertThrows(IllegalArgumentException.class, () -> MensajeChat.crear("s1", null, "hola", AHORA));
    }

    @Test
    void aceptaHastaElLargoMaximoYRechazaMas() {
        String justo = "x".repeat(MensajeChat.MAX_LONGITUD);
        assertEquals(justo, MensajeChat.crear("s1", "ana", justo, AHORA).texto());
        assertThrows(IllegalArgumentException.class,
                () -> MensajeChat.crear("s1", "ana", justo + "x", AHORA));
    }

    @Test
    void losEspaciosDeLosBordesNoCuentanParaElLargo() {
        String justo = "x".repeat(MensajeChat.MAX_LONGITUD);
        assertEquals(justo, MensajeChat.crear("s1", "ana", "   " + justo + "   ", AHORA).texto());
    }
}
