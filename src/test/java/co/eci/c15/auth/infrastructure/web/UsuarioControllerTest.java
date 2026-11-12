package co.eci.c15.auth.infrastructure.web;

import co.eci.c15.auth.application.BuscarUsuariosUseCase;
import co.eci.c15.auth.application.UsuarioDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerTest {

    private final BuscarUsuariosUseCase useCase = mock(BuscarUsuariosUseCase.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new UsuarioController(useCase)).build();

    @Test
    void idsSeparadosPorComaDevuelvenLosNombres() throws Exception {
        when(useCase.ejecutar(List.of("u1", "u2"))).thenReturn(List.of(
                new UsuarioDto("u1", "juan"), new UsuarioDto("u2", "laura")));

        mvc.perform(get("/api/usuarios").param("ids", "u1,u2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u1"))
                .andExpect(jsonPath("$[0].username").value("juan"))
                .andExpect(jsonPath("$[1].username").value("laura"));
    }

    @Test
    void sinIdsResponde400() throws Exception {
        when(useCase.ejecutar(any())).thenThrow(new IllegalArgumentException("Debes enviar al menos un id"));

        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debes enviar al menos un id"));
    }
}
