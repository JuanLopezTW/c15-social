package co.eci.c15.auth.infrastructure.web;

import co.eci.c15.auth.application.BuscarUsuariosUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final BuscarUsuariosUseCase buscarUsuarios;

    public UsuarioController(BuscarUsuariosUseCase buscarUsuarios) {
        this.buscarUsuarios = buscarUsuarios;
    }

    /** GET /api/usuarios?ids=a,b,c -> [{userId, username}]. */
    @GetMapping
    public ResponseEntity<?> buscar(@RequestParam(value = "ids", required = false) List<String> ids) {
        try {
            return ResponseEntity.ok(buscarUsuarios.ejecutar(ids));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
