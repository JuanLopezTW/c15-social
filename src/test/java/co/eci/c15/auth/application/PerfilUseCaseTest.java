package co.eci.c15.auth.application;

import co.eci.c15.auth.domain.UsernameEnUsoException;
import co.eci.c15.auth.domain.UsernameInvalidoException;
import co.eci.c15.auth.domain.Usuario;
import co.eci.c15.auth.domain.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PerfilUseCaseTest {

    private UsuarioRepository repo;
    private PerfilUseCase perfil;

    @BeforeEach
    void setUp() {
        repo = mock(UsuarioRepository.class);
        when(repo.findAllById(any())).thenReturn(List.of());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        perfil = new PerfilUseCase(repo);
    }

    @Test
    void sinPerfilLaConsultaEstaVacia() {
        assertEquals(Optional.empty(), perfil.consultar("auth0|1"));
    }

    @Test
    void registraElNombreConElIdDelProveedor() {
        assertEquals(new PerfilDto("auth0|1", "jugador1"), perfil.registrar("auth0|1", "jugador1"));

        verify(repo).save(any());
    }

    @Test
    void siYaTienePerfilConservaElNombre() {
        when(repo.findAllById(List.of("auth0|1"))).thenReturn(List.of(Usuario.registrar("auth0|1", "original")));

        assertEquals(new PerfilDto("auth0|1", "original"), perfil.registrar("auth0|1", "otro_nombre"));

        verify(repo, never()).save(any());
    }

    @Test
    void unNombreDeOtraCuentaNoSePuedeUsar() {
        when(repo.existsByUsername("ocupado")).thenReturn(true);

        assertThrows(UsernameEnUsoException.class, () -> perfil.registrar("auth0|2", "ocupado"));
        verify(repo, never()).save(any());
    }

    @Test
    void unNombreInvalidoSeRechaza() {
        assertThrows(UsernameInvalidoException.class, () -> perfil.registrar("auth0|1", "ab"));
        assertThrows(UsernameInvalidoException.class, () -> perfil.registrar("auth0|1", null));
    }
}
