package co.eci.c15.admin.infrastructure;

import co.eci.c15.admin.application.LogsNoDisponiblesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConsultaLogsAzureTest {

    private static final String IDENTIDAD = "http://identidad.local/msi/token";
    private static final String API = "https://logs.local";
    private static final String TOKEN_URL = IDENTIDAD + "?resource=https%3A%2F%2Fapi.loganalytics.io&api-version=2019-08-01";
    private static final Instant AHORA = Instant.parse("2026-11-13T10:00:00Z");
    private static final String TOKEN = "{\"access_token\":\"tk-1\",\"expires_on\":\"" + AHORA.plusSeconds(3600).getEpochSecond() + "\"}";
    private static final String RESULTADO = """
            {"tables":[{"name":"PrimaryResult","columns":[{"name":"servicio","type":"string"},{"name":"p95","type":"real"}],
             "rows":[["c15-gameplay",42.5],["c15-salas",7.0]]}]}""";

    private MockRestServiceServer azure;
    private ConsultaLogsAzure logs;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        azure = MockRestServiceServer.bindTo(builder).build();
        logs = new ConsultaLogsAzure(builder, "ws-1", IDENTIDAD, "secreto-local", "", API, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void consultaConElTokenDeLaIdentidadYDevuelveLasFilas() {
        azure.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-IDENTITY-HEADER", "secreto-local"))
                .andRespond(withSuccess(TOKEN, MediaType.APPLICATION_JSON));
        azure.expect(requestTo(API + "/v1/workspaces/ws-1/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tk-1"))
                .andExpect(jsonPath("$.query").value("una consulta"))
                .andRespond(withSuccess(RESULTADO, MediaType.APPLICATION_JSON));

        List<Map<String, Object>> filas = logs.consultar("una consulta");

        assertEquals(2, filas.size());
        assertEquals("c15-gameplay", filas.get(0).get("servicio"));
        assertEquals(42.5, filas.get(0).get("p95"));
        azure.verify();
    }

    @Test
    void elTokenSeReusaMientrasNoVence() {
        azure.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess(TOKEN, MediaType.APPLICATION_JSON));
        azure.expect(requestTo(API + "/v1/workspaces/ws-1/query")).andRespond(withSuccess(RESULTADO, MediaType.APPLICATION_JSON));
        azure.expect(requestTo(API + "/v1/workspaces/ws-1/query")).andRespond(withSuccess("{\"tables\":[]}", MediaType.APPLICATION_JSON));

        logs.consultar("primera");
        List<Map<String, Object>> vacia = logs.consultar("segunda");

        assertTrue(vacia.isEmpty());
        azure.verify();
    }

    @Test
    void siAzureFallaElAdminRecibeLogsNoDisponibles() {
        azure.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess(TOKEN, MediaType.APPLICATION_JSON));
        azure.expect(requestTo(API + "/v1/workspaces/ws-1/query")).andRespond(withServerError());

        assertThrows(LogsNoDisponiblesException.class, () -> logs.consultar("algo"));
    }

    @Test
    void sinTokenDeLaIdentidadNoSeConsulta() {
        azure.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(LogsNoDisponiblesException.class, () -> logs.consultar("algo"));
    }

    @Test
    void conUnaIdentidadPropiaSePideElTokenDeEsaIdentidad() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer conCliente = MockRestServiceServer.bindTo(builder).build();
        ConsultaLogsAzure propia = new ConsultaLogsAzure(builder, "ws-1", IDENTIDAD, "secreto-local", "cliente-1", API,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        conCliente.expect(requestTo(TOKEN_URL + "&client_id=cliente-1")).andRespond(withSuccess(TOKEN, MediaType.APPLICATION_JSON));
        conCliente.expect(requestTo(API + "/v1/workspaces/ws-1/query")).andRespond(withSuccess(RESULTADO, MediaType.APPLICATION_JSON));

        assertEquals(2, propia.consultar("algo").size());
        conCliente.verify();
    }

    @Test
    void fueraDeAzureElAdminSabePorQueNoHayLogs() {
        ConsultaLogsNoConfigurada local = new ConsultaLogsNoConfigurada();

        LogsNoDisponiblesException e = assertThrows(LogsNoDisponiblesException.class, () -> local.consultar("algo"));
        assertTrue(e.getMessage().contains("LOG_ANALYTICS_WORKSPACE"));
    }
}
