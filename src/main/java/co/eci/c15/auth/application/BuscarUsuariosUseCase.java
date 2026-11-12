package co.eci.c15.auth.application;

import co.eci.c15.auth.domain.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BuscarUsuariosUseCase {

    /** Tope de ids por consulta: una sala nunca tiene tantos jugadores. */
    public static final int MAX_IDS = 50;

    private final UsuarioRepository usuarios;

    public BuscarUsuariosUseCase(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Nombres de los usuarios pedidos (para mostrar a los demás jugadores). Los ids que no existen se omiten. */
    public List<UsuarioDto> ejecutar(Collection<String> ids) {
        Set<String> limpios = ids == null ? Set.of() : ids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());
        if (limpios.isEmpty()) {
            throw new IllegalArgumentException("Debes enviar al menos un id");
        }
        if (limpios.size() > MAX_IDS) {
            throw new IllegalArgumentException("Máximo " + MAX_IDS + " ids por consulta");
        }
        return usuarios.findAllById(limpios).stream()
                .map(u -> new UsuarioDto(u.getId(), u.getUsername()))
                .sorted(Comparator.comparing(UsuarioDto::userId))
                .toList();
    }
}
