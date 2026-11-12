package co.eci.c15.estadisticas.infrastructure.web;

import co.eci.c15.estadisticas.application.EstadisticasJugadorDto;
import co.eci.c15.estadisticas.application.EstadisticasService;
import co.eci.c15.estadisticas.application.JugadorRankingDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final EstadisticasService estadisticas;

    public EstadisticasController(EstadisticasService estadisticas) {
        this.estadisticas = estadisticas;
    }

    @GetMapping("/ranking")
    public List<JugadorRankingDto> ranking(@RequestParam(value = "limite", defaultValue = "10") int limite) {
        return estadisticas.ranking(limite);
    }

    /** Un jugador sin partidas devuelve todo en cero. */
    @GetMapping("/jugadores/{userId}")
    public EstadisticasJugadorDto deJugador(@PathVariable("userId") String userId) {
        return estadisticas.deJugador(userId);
    }
}
