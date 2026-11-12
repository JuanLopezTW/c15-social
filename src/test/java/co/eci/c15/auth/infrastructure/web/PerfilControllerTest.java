package co.eci.c15.auth.infrastructure.web;

import co.eci.c15.auth.application.PerfilDto;
import co.eci.c15.auth.application.PerfilUseCase;
import co.eci.c15.auth.domain.UsernameEnUsoException;
import co.eci.c15.auth.domain.UsernameInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PerfilControllerTest {

    private final PerfilUseCase perfil = mock(PerfilUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PerfilController(perfil)).build();
    }

    private ResultActions registrar(String username) throws Exception {
        return mvc.perform(post("/api/perfil").principal(() -> "auth0|1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\"}"));
    }

    @Test
    void consultarDevuelveElPerfilDelToken() throws Exception {
        when(perfil.consultar("auth0|1")).thenReturn(Optional.of(new PerfilDto("auth0|1", "jugador1")));

        mvc.perform(get("/api/perfil").principal(() -> "auth0|1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jugador1"));
    }

    @Test
    void sinPerfilDevuelve404() throws Exception {
        when(perfil.consultar("auth0|1")).thenReturn(Optional.empty());

        mvc.perform(get("/api/perfil").principal(() -> "auth0|1")).andExpect(status().isNotFound());
    }

    @Test
    void registrarDevuelveElPerfil() throws Exception {
        when(perfil.registrar("auth0|1", "jugador1")).thenReturn(new PerfilDto("auth0|1", "jugador1"));

        registrar("jugador1").andExpect(status().isOk()).andExpect(jsonPath("$.userId").value("auth0|1"));
    }

    @Test
    void nombreOcupadoDevuelve409() throws Exception {
        when(perfil.registrar("auth0|1", "ocupado")).thenThrow(new UsernameEnUsoException("ocupado"));

        registrar("ocupado").andExpect(status().isConflict()).andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void nombreInvalidoDevuelve400() throws Exception {
        when(perfil.registrar("auth0|1", "ab")).thenThrow(new UsernameInvalidoException("corto"));

        registrar("ab").andExpect(status().isBadRequest());
    }
}
