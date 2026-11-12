package co.eci.c15.admin.application;

import java.util.List;
import java.util.Map;

/**
 * Tablero tecnico: por servicio y replica, peticiones, p50/p95, % de errores, excepciones y ultima senal, con la
 * marca de degradada (p95 > 300 ms o errores > 2 %) o sin senal (sin logs en 2 min). Mas los usuarios concurrentes.
 */
public record TableroTecnicoDto(int minutos, List<Map<String, Object>> replicas, Object usuariosConcurrentes) {
}
