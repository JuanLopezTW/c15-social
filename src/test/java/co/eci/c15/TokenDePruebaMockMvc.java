package co.eci.c15;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * En los tests de flujo cada peticion lleva un token del jugador que actua, sacado de lo que la misma
 * prueba dice (userId, solicitanteId o anfitrionId en el cuerpo, ?userId= o /jugadores/{id}). El back
 * ignora esos datos: el jugador sale del token. SeguridadFlujoTest prueba justo eso, sin este ayudante.
 */
@Component
class TokenDePruebaMockMvc implements MockMvcBuilderCustomizer {

    private static final Pattern JUGADOR_EN_RUTA = Pattern.compile("/jugadores/([^/]+)");
    private final ObjectMapper json = new ObjectMapper();

    @Override
    public void customize(ConfigurableMockMvcBuilder<?> builder) {
        builder.defaultRequest(get("/").with(peticion ->
                jwt().jwt(token -> token.subject(jugadorDe(peticion))).postProcessRequest(peticion)));
    }

    private String jugadorDe(MockHttpServletRequest peticion) {
        String enCuerpo = jugadorEnCuerpo(peticion);
        if (enCuerpo != null) return enCuerpo;
        if (peticion.getParameter("userId") != null) return peticion.getParameter("userId");
        Matcher ruta = JUGADOR_EN_RUTA.matcher(peticion.getRequestURI());
        return ruta.find() ? ruta.group(1) : "jugador-de-prueba";
    }

    private String jugadorEnCuerpo(MockHttpServletRequest peticion) {
        byte[] cuerpo = peticion.getContentAsByteArray();
        if (cuerpo == null || cuerpo.length == 0) return null;
        try {
            JsonNode datos = json.readTree(cuerpo);
            for (String campo : new String[]{"userId", "solicitanteId", "anfitrionId"}) {
                if (datos.hasNonNull(campo)) return datos.get(campo).asText();
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }
}
