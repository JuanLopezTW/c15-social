package co.eci.c15.plataforma.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Commit que esta corriendo (lo pone el Dockerfile en APP_VERSION; "dev" fuera de la imagen del
 * deploy). La pipeline lo consulta para saber que Azure ya cambio al contenedor nuevo.
 */
@RestController
public class VersionController {

    private final String version;

    public VersionController(@Value("${APP_VERSION:dev}") String version) {
        this.version = version;
    }

    @GetMapping("/api/version")
    public Map<String, String> version() {
        return Map.of("version", version);
    }
}
