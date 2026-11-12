package co.eci.c15.admin.infrastructure;

import co.eci.c15.admin.application.ConsultaLogs;
import co.eci.c15.admin.application.LogsNoDisponiblesException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Fuera de Azure (local, pruebas) no hay logs centralizados: el admin recibe un 503 con el motivo. */
@Component
@ConditionalOnExpression("'${c15.admin.log-analytics.workspace:}'.isBlank()")
public class ConsultaLogsNoConfigurada implements ConsultaLogs {

    @Override
    public List<Map<String, Object>> consultar(String kql) {
        throw new LogsNoDisponiblesException("Los logs centralizados solo estan en Azure (falta LOG_ANALYTICS_WORKSPACE)");
    }
}
