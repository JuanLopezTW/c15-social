package co.eci.c15.admin.application;

import java.util.List;
import java.util.Map;

/** Tablero de negocio: totales del periodo, las funciones mas usadas y los jugadores activos en el tiempo. */
public record TableroNegocioDto(int minutos, Map<String, Object> totales, List<Map<String, Object>> masUsadas,
                                List<Map<String, Object>> jugadoresEnElTiempo) {
}
