package co.eci.c15.admin.application;

import java.util.List;
import java.util.Map;

/**
 * Consultas (KQL) sobre los logs centralizados de todos los servicios y sus replicas (Log Analytics en Azure).
 * Al estar todo junto, los conteos salen consolidados: una partida cuenta una vez aunque la atiendan varias replicas.
 */
public interface ConsultaLogs {

    /** Filas del resultado, cada una como columna -> valor. */
    List<Map<String, Object>> consultar(String kql);
}
