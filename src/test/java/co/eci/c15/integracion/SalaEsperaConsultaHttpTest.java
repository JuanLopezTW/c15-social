package co.eci.c15.integracion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SalaEsperaConsultaHttpTest {

    private MockRestServiceServer salasHttp;
    private SalaEsperaConsultaHttp consulta;

    @BeforeEach
    void setUp() {
        RestClient.Builder http = RestClient.builder();
        salasHttp = MockRestServiceServer.bindTo(http).build();
        consulta = new SalaEsperaConsultaHttp(http, "http://salas");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void conLaSalaEnEsperaRespondeConSusJugadoresYLlevaElTokenDelJugador() {
        Jwt jwt = Jwt.withTokenValue("token-ana").header("alg", "none").subject("auth0|ana").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        salasHttp.expect(requestTo("http://salas/api/salas/s1")).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-ana"))
                .andRespond(withSuccess("{\"id\":\"s1\",\"estado\":\"DISPONIBLE\",\"jugadores\":[\"auth0|ana\"]}",
                        MediaType.APPLICATION_JSON));
        salasHttp.expect(requestTo("http://salas/api/salas/s1"))
                .andRespond(withSuccess("{\"jugadores\":[\"auth0|ana\"]}", MediaType.APPLICATION_JSON));

        assertTrue(consulta.enEspera("s1"));
        assertFalse(consulta.contieneJugador("s1", "auth0|intruso"));
        salasHttp.verify();
    }

    @Test
    void enPartida409OInexistente404NoEstaEnEspera() {
        salasHttp.expect(requestTo("http://salas/api/salas/s2")).andRespond(withStatus(HttpStatus.CONFLICT));
        salasHttp.expect(requestTo("http://salas/api/salas/s3")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertFalse(consulta.enEspera("s2"));
        assertFalse(consulta.contieneJugador("s3", "auth0|ana"));
    }

    @Test
    void sinJugadoresEnLaRespuestaNadieEstaDentro() {
        salasHttp.expect(requestTo("http://salas/api/salas/s4")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertFalse(consulta.contieneJugador("s4", "auth0|ana"));
    }

    @Test
    void otrosErroresDeSalasNoSeEsconden() {
        salasHttp.expect(requestTo("http://salas/api/salas/s5")).andRespond(withServerError());
        salasHttp.expect(requestTo("http://salas/api/salas/s6")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(HttpServerErrorException.class, () -> consulta.enEspera("s5"));
        assertThrows(HttpClientErrorException.class, () -> consulta.enEspera("s6"));
    }
}
