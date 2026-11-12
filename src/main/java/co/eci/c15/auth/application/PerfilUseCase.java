package co.eci.c15.auth.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.auth.domain.UsernameEnUsoException;
import co.eci.c15.auth.domain.Usuario;
import co.eci.c15.auth.domain.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * El nombre de jugador de cada cuenta. Quien entra y con que contrasena lo resuelve el proveedor de
 * identidad; aqui solo se vincula su identificador (el "sub" del token) con el nombre que elige.
 */
@Service
public class PerfilUseCase {

    private static final Logger log = LoggerFactory.getLogger(PerfilUseCase.class);

    private final UsuarioRepository usuarios;

    public PerfilUseCase(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Optional<PerfilDto> consultar(String userId) {
        return usuarios.findAllById(List.of(userId)).stream()
                .findFirst()
                .map(u -> new PerfilDto(u.getId(), u.getUsername()));
    }

    /** Registra el nombre la primera vez. Si la cuenta ya tiene perfil, se queda con el que tenia. */
    public PerfilDto registrar(String userId, String username) {
        return consultar(userId).orElseGet(() -> {
            Usuario.validarUsername(username);
            if (usuarios.existsByUsername(username)) throw new UsernameEnUsoException(username);
            Usuario usuario = usuarios.save(Usuario.registrar(userId, username));
            EventoNegocio.de(log, "perfil.registrado").con("userId", userId).info();
            return new PerfilDto(usuario.getId(), usuario.getUsername());
        });
    }
}
