package co.eci.c15.admin.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TableroAdminServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-13T10:00:00Z");

    private final List<String> consultas = new ArrayList<>();
    private final List<List<Map<String, Object>>> respuestas = new ArrayList<>();
    private final ConsultaLogs logs = kql -> {
        consultas.add(kql);
        return respuestas.isEmpty() ? List.of() : respuestas.remove(0);
    };
    private final TableroAdminService tableros = new TableroAdminService(logs, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void elTableroDeNegocioJuntaTotalesFuncionesYJugadoresEnElTiempo() {
        respuestas.add(List.of(Map.of("jugadoresActivos", 12, "partidasIniciadas", 3)));
        respuestas.add(List.of(Map.of("funcion", "POST /api/partidas/{matchId}/jugadores/{userId}/movimiento", "usos", 900)));
        respuestas.add(List.of(Map.of("momento", "2026-11-13T09:55:00Z", "jugadores", 8)));

        TableroNegocioDto t = tableros.negocio(60);

        assertEquals(60, t.minutos());
        assertEquals(12, t.totales().get("jugadoresActivos"));
        assertEquals(900, t.masUsadas().get(0).get("usos"));
        assertEquals(1, t.jugadoresEnElTiempo().size());
        assertTrue(consultas.get(0).contains("ago(60m)"));
        assertTrue(consultas.get(0).contains("partida.iniciada"));
        assertTrue(consultas.get(2).contains("bin(TimeGenerated, 5m)"), "unos 12 puntos en la serie");
    }

    @Test
    void elPeriodoSeAcotaYUnResultadoVacioNoRompe() {
        TableroNegocioDto corto = tableros.negocio(1);
        TableroNegocioDto largo = tableros.negocio(100_000);

        assertEquals(TableroAdminService.MIN_MINUTOS, corto.minutos());
        assertEquals(TableroAdminService.MAX_MINUTOS, largo.minutos());
        assertTrue(corto.totales().isEmpty());
        assertTrue(consultas.stream().anyMatch(k -> k.contains("bin(TimeGenerated, 1m)")), "el intervalo no baja de 1 minuto");
    }

    @Test
    void elTableroTecnicoMarcaLasReplicasDegradadasYLasQueDejaronDeReportar() {
        respuestas.add(List.of(
                Map.of("servicio", "c15-gameplay", "replica", "gp-1", "peticiones", 1000, "p50", 20.0, "p95", 80.0, "errores5xx", 0),
                Map.of("servicio", "c15-gameplay", "replica", "gp-2", "peticiones", 1000, "p50", 200.0, "p95", 450.0, "errores5xx", 5),
                Map.of("servicio", "c15-salas", "replica", "sa-1", "peticiones", 100, "p50", 10.0, "p95", 30.0, "errores5xx", 3)));
        respuestas.add(List.of(
                Map.of("servicio", "c15-gameplay", "replica", "gp-1", "ultimaSenal", "2026-11-13T09:59:30Z", "excepciones", 0),
                Map.of("servicio", "c15-gameplay", "replica", "gp-2", "ultimaSenal", "2026-11-13T09:59:50Z", "excepciones", 2),
                Map.of("servicio", "c15-salas", "replica", "sa-1", "ultimaSenal", "2026-11-13T09:59:00Z", "excepciones", 0),
                Map.of("servicio", "c15-social", "replica", "so-1", "ultimaSenal", "2026-11-13T09:50:00Z", "excepciones", 0)));
        respuestas.add(List.of(Map.of("usuariosConcurrentes", 7)));

        TableroTecnicoDto t = tableros.tecnico(15);

        Map<String, Map<String, Object>> r = new java.util.HashMap<>();
        t.replicas().forEach(x -> r.put((String) x.get("replica"), x));
        assertEquals(false, r.get("gp-1").get("degradada"));
        assertEquals(true, r.get("gp-2").get("degradada"), "p95 > 300 ms");
        assertEquals(true, r.get("sa-1").get("degradada"), "3 % de errores > 2 %");
        assertEquals(3.0, r.get("sa-1").get("porcentajeErrores"));
        assertEquals(false, r.get("gp-1").get("sinSenal"));
        assertEquals(true, r.get("so-1").get("sinSenal"), "10 min sin logs");
        assertEquals(0, r.get("so-1").get("peticiones"), "una replica sin peticiones igual aparece");
        assertEquals(7, t.usuariosConcurrentes());
        assertEquals("c15-gameplay", t.replicas().get(0).get("servicio"), "ordenadas por servicio y replica");
    }

    @Test
    void unaReplicaSinUltimaSenalCuentaComoSinSenal() {
        respuestas.add(List.of(Map.of("servicio", "c15-broker", "replica", "br-1", "peticiones", 10, "p95", 5.0, "errores5xx", 0)));

        TableroTecnicoDto t = tableros.tecnico(15);

        assertEquals(true, t.replicas().get(0).get("sinSenal"));
        assertEquals(0, t.usuariosConcurrentes());
    }

    @Test
    void elErrorSeBuscaPorElCodigoQueVeElJugador() {
        respuestas.add(List.of(Map.of("servicio", "c15-gameplay", "excepcion", "java.lang.IllegalStateException")));

        List<Map<String, Object>> filas = tableros.errores("ABCD1234", 30);

        assertEquals(1, filas.size());
        assertTrue(consultas.get(0).contains("startswith 'abcd1234'"), "en minusculas, como el traceId");
        assertTrue(consultas.get(0).contains("ago(7d)"), "como mucho 7 dias");
    }

    @Test
    void unCodigoQueNoEsHexadecimalNoLlegaALaConsulta() {
        assertThrows(IllegalArgumentException.class, () -> tableros.errores("abcd' or 1==1 //", 1));
        assertThrows(IllegalArgumentException.class, () -> tableros.errores("abc", 1));
        assertThrows(IllegalArgumentException.class, () -> tableros.errores(null, 1));
        assertTrue(consultas.isEmpty());
    }
}
