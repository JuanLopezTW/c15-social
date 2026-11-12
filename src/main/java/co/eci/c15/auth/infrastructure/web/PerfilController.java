package co.eci.c15.auth.infrastructure.web;

import co.eci.c15.auth.application.PerfilUseCase;
import co.eci.c15.auth.domain.UsernameEnUsoException;
import co.eci.c15.auth.domain.UsernameInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/** Perfil de quien trae el token. 404 en GET: entro por primera vez y aun no tiene nombre de jugador. */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilUseCase perfil;

    public PerfilController(PerfilUseCase perfil) {
        this.perfil = perfil;
    }

    @GetMapping
    public ResponseEntity<?> consultar(Principal yo) {
        return perfil.consultar(yo.getName())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Esta cuenta aun no tiene nombre de jugador")));
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Map<String, String> body, Principal yo) {
        try {
            return ResponseEntity.ok(perfil.registrar(yo.getName(), body.get("username")));
        } catch (UsernameInvalidoException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (UsernameEnUsoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
