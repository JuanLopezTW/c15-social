package co.eci.c15.auth.application;

import co.eci.c15.auth.domain.Usuario;
import co.eci.c15.auth.domain.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BuscarUsuariosUseCaseTest {

    private UsuarioRepository repo;
    private BuscarUsuariosUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = mock(UsuarioRepository.class);
        useCase = new BuscarUsuariosUseCase(repo);
    }

    @Test
    void devuelveLosNombresOrdenadosPorId() {
        when(repo.findAllById(any())).thenReturn(List.of(
                Usuario.reconstituir("u2", "laura"), Usuario.reconstituir("u1", "juan")));

        List<UsuarioDto> usuarios = useCase.ejecutar(List.of("u1", "u2"));

        assertEquals(List.of(new UsuarioDto("u1", "juan"), new UsuarioDto("u2", "laura")), usuarios);
    }

    @Test
    @SuppressWarnings("unchecked")
    void quitaRepetidosYVaciosAntesDeConsultar() {
        when(repo.findAllById(any())).thenReturn(List.of());
        ArgumentCaptor<Collection<String>> ids = ArgumentCaptor.forClass(Collection.class);

        useCase.ejecutar(Arrays.asList("u1", " u1 ", "", null, "u2"));

        verify(repo).findAllById(ids.capture());
        assertEquals(Set.of("u1", "u2"), Set.copyOf(ids.getValue()));
    }

    @Test
    void sinIdsLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar(List.of(" ")));
        verifyNoInteractions(repo);
    }

    @Test
    void masDelTopeLanzaExcepcion() {
        List<String> ids = new ArrayList<>(IntStream.rangeClosed(0, BuscarUsuariosUseCase.MAX_IDS)
                .mapToObj(i -> "u" + i).toList());
        assertThrows(IllegalArgumentException.class, () -> useCase.ejecutar(ids));
        verifyNoInteractions(repo);
    }
}
