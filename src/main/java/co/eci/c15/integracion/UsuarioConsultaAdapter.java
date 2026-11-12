package co.eci.c15.integracion;

import co.eci.c15.auth.domain.UsuarioRepository;
import co.eci.c15.chat.application.UsuarioConsulta;
import org.springframework.stereotype.Component;

import java.util.List;

/** Conecta el chat con los usuarios (los dos viven en este servicio) sin que un modulo dependa del otro. */
@Component
public class UsuarioConsultaAdapter implements UsuarioConsulta {

    private final UsuarioRepository usuarios;

    public UsuarioConsultaAdapter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public boolean existe(String userId) {
        return !usuarios.findAllById(List.of(userId)).isEmpty();
    }
}
