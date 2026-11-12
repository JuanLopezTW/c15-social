package co.eci.c15.plataforma.observabilidad;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RegistroPeticionesFilterTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(RegistroPeticionesFilter.class);
    private final ListAppender<ILoggingEvent> eventos = new ListAppender<>();
    @SuppressWarnings("unchecked")
    private final RegistroPeticionesFilter filtro = new RegistroPeticionesFilter(mock(ObjectProvider.class));

    @BeforeEach
    void setUp() {
        eventos.start();
        logger.addAppender(eventos);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(eventos);
        logger.setLevel(null);
        MDC.clear();
    }

    private ILoggingEvent pedir(String metodo, String uri, int estado) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest(metodo, uri);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, (req, res) -> respuesta.setStatus(estado));
        return eventos.list.get(eventos.list.size() - 1);
    }

    private static Map<String, Object> campos(ILoggingEvent evento) {
        return evento.getKeyValuePairs().stream().collect(Collectors.toMap(k -> k.key, k -> k.value));
    }

    @Test
    void registraMetodoRutaEstadoYDuracion() throws Exception {
        ILoggingEvent evento = pedir("GET", "/api/salas", 200);

        assertEquals(Level.INFO, evento.getLevel());
        assertTrue(evento.getFormattedMessage().startsWith("GET /api/salas -> 200 en "), evento.getFormattedMessage());
        Map<String, Object> campos = campos(evento);
        assertEquals("http.peticion", campos.get("evento"));
        assertEquals(200, campos.get("http.estado"));
        assertTrue(campos.containsKey("duracion.ms"));
    }

    @Test
    void usaLaPlantillaDeRutaNoLaUriReal() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/salas/abc");
        peticion.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/salas/{salaId}");

        filtro.doFilter(peticion, new MockHttpServletResponse(), mock(FilterChain.class));

        assertEquals("/api/salas/{salaId}", campos(eventos.list.get(0)).get("http.ruta"));
    }

    @Test
    void elNivelDependeDelEstado() throws Exception {
        assertEquals(Level.INFO, pedir("POST", "/api/salas", 409).getLevel());
        assertEquals(Level.WARN, pedir("GET", "/api/salas", 401).getLevel());
        assertEquals(Level.WARN, pedir("GET", "/api/salas", 403).getLevel());
        assertEquals(Level.ERROR, pedir("GET", "/api/salas", 503).getLevel());
    }

    @Test
    void losClicksVanEnDebugSalvoQueFallen() throws Exception {
        assertEquals(Level.DEBUG, pedir("POST", "/api/partidas/m1/reto/clicks", 204).getLevel());
        assertEquals(Level.DEBUG, pedir("POST", "/api/partidas/m1/acertijos/a1/clicks", 200).getLevel());
        assertEquals(Level.ERROR, pedir("POST", "/api/partidas/m1/reto/clicks", 500).getLevel());
    }

    @Test
    void ponePartidaOSalaEnElContextoYLoLimpiaAlTerminar() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(new MockHttpServletRequest("GET", "/api/partidas/m-1/mapa"), respuesta, (req, res) -> {
            assertEquals("m-1", MDC.get(ContextoLog.MATCH_ID));
            MDC.put(ContextoLog.USER_ID, "auth0|ana");
        });
        assertEquals("m-1", eventos.list.get(0).getMDCPropertyMap().get(ContextoLog.MATCH_ID));
        assertEquals("auth0|ana", eventos.list.get(0).getMDCPropertyMap().get(ContextoLog.USER_ID));
        assertNull(MDC.get(ContextoLog.MATCH_ID));
        assertNull(MDC.get(ContextoLog.USER_ID));

        filtro.doFilter(new MockHttpServletRequest("GET", "/api/chat/salas/s-1"), respuesta,
                (req, res) -> assertEquals("s-1", MDC.get(ContextoLog.SALA_ID)));
        filtro.doFilter(new MockHttpServletRequest("POST", "/dev/partidas/m-2/inicio"), respuesta,
                (req, res) -> assertEquals("m-2", MDC.get(ContextoLog.MATCH_ID)));
    }

    @Test
    void unaExcepcionSeRegistraComoErrorConSuCausaYSePropaga() {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/salas");
        IllegalStateException fallo = new IllegalStateException("se rompio");

        assertSame(fallo, assertThrows(IllegalStateException.class, () -> filtro.doFilter(peticion,
                new MockHttpServletResponse(), (req, res) -> {
                    throw fallo;
                })));

        ILoggingEvent evento = eventos.list.get(0);
        assertEquals(Level.ERROR, evento.getLevel());
        assertEquals(500, campos(evento).get("http.estado"));
        assertEquals("se rompio", evento.getThrowableProxy().getMessage());
    }

    @Test
    void unaUriDelClienteNoPuedeInyectarLineasEnElLog() throws Exception {
        ILoggingEvent evento = pedir("GET", "/nada\n{\"falso\":1}", 404);

        assertFalse(evento.getFormattedMessage().contains("\n"));
        assertFalse(evento.getFormattedMessage().contains("{"));
    }

    @Test
    void noRegistraElWebSocketNiElActuator() throws Exception {
        filtro.doFilter(new MockHttpServletRequest("GET", "/ws"), new MockHttpServletResponse(), mock(FilterChain.class));
        filtro.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), new MockHttpServletResponse(), mock(FilterChain.class));

        assertTrue(eventos.list.isEmpty());
    }
}
