package co.eci.c15.plataforma.observabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Pone en el contexto de log al jugador del token. Va dentro de la cadena de seguridad (no es un bean,
 * para que Spring no lo registre tambien como filtro global); RegistroPeticionesFilter lo lee y lo limpia.
 */
public class ContextoUsuarioFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.isAuthenticated()
                && !(autenticacion instanceof AnonymousAuthenticationToken)) {
            MDC.put(ContextoLog.USER_ID, autenticacion.getName());
        }
        chain.doFilter(request, response);
    }
}
