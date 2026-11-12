package co.eci.c15.plataforma.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Roles de la app (jugador / admin). Auth0 los pone en el token con una Action, en un claim con nombre de URL
 * (Auth0 lo exige para los claims propios). Cada rol pasa a ser ROLE_<rol>; sin el claim no hay roles: es jugador.
 */
@Configuration
public class AutorizacionConfig {

    /** Rol que pueden usar las funciones de administracion (/dev/**, /api/admin/**). */
    public static final String ADMIN = "admin";
    /** Rutas solo para admin. */
    public static final String[] RUTAS_ADMIN = {"/dev/**", "/api/admin/**"};

    @Bean
    public JwtAuthenticationConverter rolesDelToken(
            @Value("${c15.seguridad.claim-roles:https://api.escaperoomc15/roles}") String claim) {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(claim);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }
}
