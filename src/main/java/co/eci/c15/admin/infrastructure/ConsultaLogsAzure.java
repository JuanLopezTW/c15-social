package co.eci.c15.admin.infrastructure;

import co.eci.c15.admin.application.ConsultaLogs;
import co.eci.c15.admin.application.LogsNoDisponiblesException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Log Analytics por su API REST, con la identidad administrada de la Container App (sin claves guardadas): Azure
 * la expone en IDENTITY_ENDPOINT / IDENTITY_HEADER, AZURE_CLIENT_ID dice cual usar, y Terraform le da solo permiso
 * de lectura sobre los logs.
 */
@Component
@ConditionalOnExpression("!'${c15.admin.log-analytics.workspace:}'.isBlank()")
// Nunca convive con ConsultaLogsNoConfigurada (lo decide c15.admin.log-analytics.workspace); @Primary solo le
// aclara al analisis estatico cual usar, porque no evalua la condicion.
@Primary
public class ConsultaLogsAzure implements ConsultaLogs {

    static final String RECURSO = "https://api.loganalytics.io";
    private static final long MARGEN_SEGUNDOS = 300;

    private final RestClient http;
    private final ObjectMapper json = new ObjectMapper();
    private final String workspace;
    private final String identidadUrl;
    private final String identidadHeader;
    private final String identidadCliente;
    private final String api;
    private final Clock reloj;
    private String token;
    private Instant venceToken = Instant.EPOCH;

    @Autowired // hay otro constructor (el de las pruebas): sin esto Spring busca uno vacio y no arranca
    public ConsultaLogsAzure(RestClient.Builder http,
                             @Value("${c15.admin.log-analytics.workspace}") String workspace,
                             @Value("${IDENTITY_ENDPOINT:}") String identidadUrl,
                             @Value("${IDENTITY_HEADER:}") String identidadHeader,
                             @Value("${AZURE_CLIENT_ID:}") String identidadCliente,
                             @Value("${c15.admin.log-analytics.url:" + RECURSO + "}") String api) {
        this(http, workspace, identidadUrl, identidadHeader, identidadCliente, api, Clock.systemUTC());
    }

    @SuppressWarnings("java:S107") // las 7 piezas de la conexion; las pruebas pasan el reloj
    ConsultaLogsAzure(RestClient.Builder http, String workspace, String identidadUrl, String identidadHeader,
                      String identidadCliente, String api, Clock reloj) {
        this.http = http.build();
        this.workspace = workspace;
        this.identidadUrl = identidadUrl;
        this.identidadHeader = identidadHeader;
        this.identidadCliente = identidadCliente;
        this.api = api;
        this.reloj = reloj;
    }

    @Override
    public List<Map<String, Object>> consultar(String kql) {
        try {
            JsonNode respuesta = http.post()
                    .uri(api + "/v1/workspaces/{workspace}/query", workspace)
                    .header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", kql))
                    .retrieve()
                    .body(JsonNode.class);
            return filas(respuesta);
        } catch (RestClientException e) {
            throw new LogsNoDisponiblesException("No se pudieron consultar los logs", e);
        }
    }

    /** Token de la identidad administrada; se reusa hasta 5 min antes de que venza. */
    private synchronized String token() {
        if (token != null && reloj.instant().isBefore(venceToken)) return token;
        JsonNode respuesta = http.get()
                .uri(identidadUrl + "?resource={recurso}&api-version=2019-08-01"
                        + (identidadCliente.isBlank() ? "" : "&client_id={cliente}"), RECURSO, identidadCliente)
                .header("X-IDENTITY-HEADER", identidadHeader)
                .retrieve()
                .body(JsonNode.class);
        if (respuesta == null || !respuesta.hasNonNull("access_token")) {
            throw new LogsNoDisponiblesException("La identidad administrada no devolvio un token");
        }
        token = respuesta.get("access_token").asText();
        venceToken = Instant.ofEpochSecond(respuesta.path("expires_on").asLong(0)).minusSeconds(MARGEN_SEGUNDOS);
        return token;
    }

    private List<Map<String, Object>> filas(JsonNode respuesta) {
        List<Map<String, Object>> filas = new ArrayList<>();
        JsonNode tabla = respuesta == null ? null : respuesta.path("tables").path(0);
        if (tabla == null || tabla.isMissingNode()) return filas;
        List<String> columnas = new ArrayList<>();
        tabla.path("columns").forEach(c -> columnas.add(c.path("name").asText()));
        for (JsonNode fila : tabla.path("rows")) {
            Map<String, Object> valores = new LinkedHashMap<>();
            for (int i = 0; i < columnas.size(); i++) valores.put(columnas.get(i), json.convertValue(fila.get(i), Object.class));
            filas.add(valores);
        }
        return filas;
    }
}
