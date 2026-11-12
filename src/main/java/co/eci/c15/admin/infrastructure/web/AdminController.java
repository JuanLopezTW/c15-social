package co.eci.c15.admin.infrastructure.web;

import co.eci.c15.admin.application.LogsNoDisponiblesException;
import co.eci.c15.admin.application.TableroAdminService;
import co.eci.c15.admin.application.TableroNegocioDto;
import co.eci.c15.admin.application.TableroTecnicoDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Solo para el rol admin (lo exigen el gateway y SeguridadConfig para /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String ERROR = "error";

    private final TableroAdminService tableros;

    public AdminController(TableroAdminService tableros) {
        this.tableros = tableros;
    }

    @GetMapping("/tablero/negocio")
    public TableroNegocioDto negocio(@RequestParam(name = "minutos", defaultValue = "60") int minutos) {
        return tableros.negocio(minutos);
    }

    @GetMapping("/tablero/tecnico")
    public TableroTecnicoDto tecnico(@RequestParam(name = "minutos", defaultValue = "15") int minutos) {
        return tableros.tecnico(minutos);
    }

    /** El codigo que el front le muestra al jugador cuando algo falla (inicio del traceId). */
    @GetMapping("/errores/{codigo}")
    public List<Map<String, Object>> errores(@PathVariable("codigo") String codigo,
                                             @RequestParam(name = "dias", defaultValue = "1") int dias) {
        return tableros.errores(codigo, dias);
    }

    @ExceptionHandler(LogsNoDisponiblesException.class)
    ResponseEntity<Map<String, String>> sinLogs(LogsNoDisponiblesException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(ERROR, e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(ERROR, e.getMessage()));
    }
}
