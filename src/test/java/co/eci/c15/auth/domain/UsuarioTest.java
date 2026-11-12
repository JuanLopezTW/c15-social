package co.eci.c15.auth.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void creaUsuarioValido() {
        Usuario u = Usuario.registrar("auth0|1", "jugador1");
        assertEquals("jugador1", u.getUsername());
        assertNotNull(u.getId());
    }

    @Test
    void rechazaUsernameVacio() {
        assertThrows(UsernameInvalidoException.class, () -> Usuario.registrar("auth0|1", ""));
        assertThrows(UsernameInvalidoException.class, () -> Usuario.registrar("auth0|1", "   "));
        assertThrows(UsernameInvalidoException.class, () -> Usuario.registrar("auth0|1", null));
    }

    @Test
    void rechazaUsernameInvalido() {
        assertThrows(UsernameInvalidoException.class, () -> Usuario.registrar("auth0|1", "ab")); // muy corto
        assertThrows(UsernameInvalidoException.class, () -> Usuario.registrar("auth0|1", "nombre con espacios"));
        assertThrows(UsernameInvalidoException.class, () -> Usuario.registrar("auth0|1", "nombre@invalido"));
    }

    @Test
    void aceptaUsernameConGuionBajo() {
        assertDoesNotThrow(() -> Usuario.registrar("auth0|1", "jugador_1"));
    }
}
