package co.eci.c15.auth.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    boolean existsByUsername(String username);
    Usuario save(Usuario usuario);
    Optional<Usuario> findByUsername(String username);
    /** Los usuarios con esos ids; los que no existen se omiten. */
    List<Usuario> findAllById(Collection<String> ids);
}
