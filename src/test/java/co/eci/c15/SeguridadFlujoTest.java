package co.eci.c15;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La seguridad real, sin el ayudante de tokens de los otros flujos: el jugador sale del token y de nada mas. */
@SpringBootTest
class SeguridadFlujoTest {

    @Autowired
    private WebApplicationContext contexto;

    /** Sin esto el primer token falso iria a buscar las llaves del proveedor por la red. */
    @MockitoBean
    private JwtDecoder tokens;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(tokens.decode("token-falso")).thenThrow(new BadJwtException("firma invalida"));
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private static RequestPostProcessor como(String sub) {
        return jwt().jwt(token -> token.subject(sub));
    }

    @Test
    void sinTokenOConTokenInvalidoLaApiNoResponde() throws Exception {
        mvc.perform(get("/api/chat/global")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/estadisticas/ranking")).andExpect(status().isUnauthorized());
        // Escribir sin token ni siquiera pasa CSRF
        mvc.perform(post("/api/perfil").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/perfil").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laVersionNoPideToken() throws Exception {
        mvc.perform(get("/api/version")).andExpect(status().isOk());
    }

    @Test
    void cadaCuentaRegistraSuNombreUnaSolaVez() throws Exception {
        mvc.perform(get("/api/perfil").with(como("auth0|nuevo"))).andExpect(status().isNotFound());

        // El userId del cuerpo se ignora: manda el del token
        mvc.perform(post("/api/perfil").with(como("auth0|nuevo")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"auth0|otro\",\"username\":\"jugador_nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("auth0|nuevo"));

        mvc.perform(get("/api/perfil").with(como("auth0|nuevo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jugador_nuevo"));
        mvc.perform(post("/api/perfil").with(como("auth0|otro")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"jugador_nuevo\"}"))
                .andExpect(status().isConflict());
    }
}
