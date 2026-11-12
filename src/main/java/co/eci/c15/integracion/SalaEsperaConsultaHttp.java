package co.eci.c15.integracion;

import co.eci.c15.chat.application.SalaEsperaConsulta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * El chat de la sala de espera le pregunta al servicio de salas por {@code GET /api/salas/{salaId}}, con el
 * mismo token del jugador que esta chateando: 200 = la sala esta en espera (y trae sus jugadores),
 * 409 = ya esta en partida, 404 = no existe. Si salas no responde, el error sube (el chat da 5xx).
 */
@Component
public class SalaEsperaConsultaHttp implements SalaEsperaConsulta {

    private final RestClient salas;

    public SalaEsperaConsultaHttp(RestClient.Builder http, @Value("${c15.servicios.salas}") String urlSalas) {
        this.salas = http.baseUrl(urlSalas).build();
    }

    @Override
    public boolean enEspera(String salaId) {
        return detalle(salaId).isPresent();
    }

    @Override
    public boolean contieneJugador(String salaId, String userId) {
        return detalle(salaId).map(s -> s.jugadores() != null && s.jugadores().contains(userId)).orElse(false);
    }

    private Optional<DetalleSala> detalle(String salaId) {
        try {
            return Optional.ofNullable(salas.get().uri("/api/salas/{salaId}", salaId)
                    .headers(this::conTokenDelJugador)
                    .retrieve()
                    .body(DetalleSala.class));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.CONFLICT) return Optional.empty();
            throw e;
        }
    }

    private void conTokenDelJugador(HttpHeaders headers) {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            headers.setBearerAuth(token.getToken().getTokenValue());
        }
    }

    /** Lo unico que el chat necesita del detalle de la sala (el resto de campos se ignora). */
    record DetalleSala(List<String> jugadores) {
    }
}
