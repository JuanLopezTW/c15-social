package co.eci.c15.admin.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lo que ve el admin dentro del app: el tablero de negocio, el tecnico y la busqueda de un error por el codigo que
 * le muestra el front al jugador. Todo sale de los logs centralizados, asi que esta consolidado entre replicas.
 */
@Service
public class TableroAdminService {

    static final int MIN_MINUTOS = 5;
    static final int MAX_MINUTOS = 1440;
    static final int MAX_DIAS = 7;
    /** Umbrales de una replica degradada (escenario O2). */
    static final double P95_MAXIMO_MS = 300;
    static final double ERRORES_MAXIMO_PCT = 2;
    /** Una replica sin ningun log en este tiempo dejo de reportar (escenario O2). */
    static final Duration SIN_SENAL = Duration.ofMinutes(2);

    /** El codigo que ve el jugador son los primeros caracteres del traceId (hexadecimal). Nada mas entra al KQL. */
    private static final Pattern CODIGO = Pattern.compile("[0-9a-f]{8,32}");

    private static final String LOGS = """
            ContainerAppConsoleLogs_CL
            | where TimeGenerated > ago(%dm)
            | extend j = parse_json(Log_s)
            """;
    /** Peticiones de los jugadores en todos los puntos: /api/version la llaman las comprobaciones de salud. */
    private static final String PETICIONES_TODAS = LOGS
            + "| where tostring(j.evento) == 'http.peticion' and tostring(j.http.ruta) != '/api/version'\n";
    /** Solo en los servicios: cada peticion cuenta una vez (el gateway tambien la registra al pasarla). */
    private static final String SIN_GATEWAY = "| where ContainerAppName_s != 'c15-gateway'\n";
    private static final String PETICIONES = PETICIONES_TODAS + SIN_GATEWAY;
    private static final String REPLICA = "replica";
    private static final String SERVICIO = "servicio";
    private static final String PETICIONES_CAMPO = "peticiones";

    private final ConsultaLogs logs;
    private final Clock reloj;

    @Autowired
    public TableroAdminService(ConsultaLogs logs) {
        this(logs, Clock.systemUTC());
    }

    TableroAdminService(ConsultaLogs logs, Clock reloj) {
        this.logs = logs;
        this.reloj = reloj;
    }

    public TableroNegocioDto negocio(int minutos) {
        int m = acotar(minutos);
        Map<String, Object> totales = primera(logs.consultar(LOGS.formatted(m) + SIN_GATEWAY + """
                | extend evento = tostring(j.evento)
                | summarize
                    jugadoresActivos = dcountif(tostring(j.userId), evento == 'http.peticion' and isnotempty(tostring(j.userId))),
                    sesionesIniciadas = countif(evento == 'stomp.conectado'),
                    jugadoresNuevos = countif(evento == 'perfil.registrado'),
                    salasCreadas = countif(evento == 'sala.creada'),
                    partidasIniciadas = dcountif(tostring(j.matchId), evento == 'partida.iniciada'),
                    partidasTerminadas = dcountif(tostring(j.matchId), evento == 'partida.terminada'),
                    acertijosResueltos = countif(evento == 'acertijo.resuelto'),
                    objetosRecolectados = countif(evento == 'objeto.recolectado'),
                    retosJugados = countif(evento == 'reto.terminado'),
                    interacciones = countif(evento == 'http.peticion' and tostring(j.http.metodo) != 'GET')
                """));
        List<Map<String, Object>> masUsadas = logs.consultar(PETICIONES.formatted(m) + """
                | summarize usos = count() by funcion = strcat(tostring(j.http.metodo), ' ', tostring(j.http.ruta))
                | top 8 by usos
                """);
        List<Map<String, Object>> jugadoresEnElTiempo = logs.consultar(PETICIONES.formatted(m) + """
                | where isnotempty(tostring(j.userId))
                | summarize jugadores = dcount(tostring(j.userId)) by momento = bin(TimeGenerated, %dm)
                | order by momento asc
                """.formatted(intervalo(m)));
        return new TableroNegocioDto(m, totales, masUsadas, jugadoresEnElTiempo);
    }

    public TableroTecnicoDto tecnico(int minutos) {
        int m = acotar(minutos);
        List<Map<String, Object>> peticiones = logs.consultar(PETICIONES_TODAS.formatted(m) + """
                | extend ms = todouble(j.duracion.ms), estado = toint(j.http.estado)
                | summarize peticiones = count(), p50 = percentile(ms, 50), p95 = percentile(ms, 95),
                    errores5xx = countif(estado >= 500) by servicio = ContainerAppName_s, replica = ContainerGroupName_s
                """);
        // Solo las replicas de la version que corre cada servicio: las reemplazadas por un deploy no cuentan
        List<Map<String, Object>> senales = logs.consultar(LOGS.formatted(m) + """
                | summarize ultimaSenal = max(TimeGenerated), excepciones = countif(tostring(j.log.level) == 'ERROR')
                    by servicio = ContainerAppName_s, replica = ContainerGroupName_s, revision = RevisionName_s
                | join kind=inner (
                    ContainerAppConsoleLogs_CL
                    | where TimeGenerated > ago(%dm)
                    | summarize arg_max(TimeGenerated, RevisionName_s) by servicio = ContainerAppName_s
                    | project servicio, revisionActual = RevisionName_s) on servicio
                | where revision == revisionActual
                | project servicio, replica, ultimaSenal, excepciones
                """.formatted(m));
        Map<String, Object> concurrentes = primera(logs.consultar(PETICIONES.formatted(MIN_MINUTOS) + """
                | summarize usuariosConcurrentes = dcount(tostring(j.userId))
                """));
        return new TableroTecnicoDto(m, replicas(peticiones, senales), concurrentes.getOrDefault("usuariosConcurrentes", 0));
    }

    /** Todo lo que dejo la peticion con ese codigo, en orden: que servicio y replica la atendio y la excepcion. */
    public List<Map<String, Object>> errores(String codigo, int dias) {
        String c = codigo == null ? "" : codigo.trim().toLowerCase();
        if (!CODIGO.matcher(c).matches()) {
            throw new IllegalArgumentException("Codigo invalido: son 8 a 32 caracteres hexadecimales");
        }
        int d = Math.clamp(dias, 1, MAX_DIAS);
        return logs.consultar("""
                ContainerAppConsoleLogs_CL
                | where TimeGenerated > ago(%dd)
                | where Log_s contains '%s'
                | extend j = parse_json(Log_s)
                | where tostring(j.traceId) startswith '%s'
                | project hora = TimeGenerated, servicio = ContainerAppName_s, replica = ContainerGroupName_s,
                    nivel = tostring(j.log.level), mensaje = tostring(j.message), traceId = tostring(j.traceId),
                    ruta = tostring(j.http.ruta), estado = toint(j.http.estado), excepcion = tostring(j.error.type),
                    detalle = substring(tostring(j.error.message), 0, 500), pila = substring(tostring(j.error.stack_trace), 0, 2000)
                | order by hora asc
                | take 50
                """.formatted(d, c, c));
    }

    /**
     * A cada replica actual (las de senales) le suma sus peticiones y la marca degradada o sin senal. Las peticiones
     * de replicas que ya no estan (reemplazadas por un deploy) no se muestran.
     */
    private List<Map<String, Object>> replicas(List<Map<String, Object>> peticiones, List<Map<String, Object>> senales) {
        Map<String, Map<String, Object>> porReplica = new LinkedHashMap<>();
        for (Map<String, Object> s : senales) porReplica.put(clave(s), new LinkedHashMap<>(s));
        for (Map<String, Object> p : peticiones) {
            Map<String, Object> r = porReplica.get(clave(p));
            if (r != null) r.putAll(p);
        }
        Instant ahora = reloj.instant();
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map<String, Object> r : porReplica.values()) {
            long total = numero(r.get(PETICIONES_CAMPO)).longValue();
            double errores = total == 0 ? 0 : 100.0 * numero(r.get("errores5xx")).doubleValue() / total;
            r.putIfAbsent(PETICIONES_CAMPO, 0);
            r.put("porcentajeErrores", Math.round(errores * 100) / 100.0);
            r.put("degradada", numero(r.get("p95")).doubleValue() > P95_MAXIMO_MS || errores > ERRORES_MAXIMO_PCT);
            Object ultima = r.get("ultimaSenal");
            r.put("sinSenal", ultima == null || Duration.between(Instant.parse(ultima.toString()), ahora).compareTo(SIN_SENAL) > 0);
            resultado.add(r);
        }
        resultado.sort(Comparator.comparing(TableroAdminService::clave));
        return resultado;
    }

    private static String clave(Map<String, Object> fila) {
        return fila.get(SERVICIO) + "/" + fila.get(REPLICA);
    }

    private static Number numero(Object valor) {
        return valor instanceof Number n ? n : 0;
    }

    private static Map<String, Object> primera(List<Map<String, Object>> filas) {
        return filas.isEmpty() ? Map.of() : filas.get(0);
    }

    private static int acotar(int minutos) {
        return Math.clamp(minutos, MIN_MINUTOS, MAX_MINUTOS);
    }

    /** Unos 12 puntos en la serie, sin bajar de 1 minuto. */
    private static int intervalo(int minutos) {
        return Math.max(1, minutos / 12);
    }
}
