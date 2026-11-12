package co.eci.c15.plataforma.config;

import co.eci.c15.plataforma.observabilidad.ContextoUsuarioFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * La API solo atiende peticiones con un token valido del proveedor de identidad; el jugador sale de
 * ahi (el "sub" del token), nunca de lo que mande el cliente. El gateway ya valida el token, pero cada
 * servicio lo vuelve a validar: si alguien llega directo al servicio, tampoco pasa.
 * Las funciones de administracion (/dev/**, /api/admin/**) piden el rol admin del token: un jugador recibe 403.
 * CSRF sigue activo: oauth2ResourceServer ya exime a las peticiones con token Bearer (no usan cookies).
 */
@Configuration
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain seguridad(HttpSecurity http, JwtAuthenticationConverter rolesDelToken) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/version", "/error").permitAll()
                        .requestMatchers(AutorizacionConfig.RUTAS_ADMIN).hasRole(AutorizacionConfig.ADMIN)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(rolesDelToken)))
                .addFilterAfter(new ContextoUsuarioFilter(), BearerTokenAuthenticationFilter.class)
                .build();
    }
}
