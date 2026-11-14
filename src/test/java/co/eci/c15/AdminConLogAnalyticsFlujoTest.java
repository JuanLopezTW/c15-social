package co.eci.c15;

import co.eci.c15.admin.application.ConsultaLogs;
import co.eci.c15.admin.infrastructure.ConsultaLogsAzure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Como en Azure: con el workspace configurado la app arranca y el admin consulta Log Analytics. */
@SpringBootTest(properties = "c15.admin.log-analytics.workspace=00000000-0000-0000-0000-000000000000")
class AdminConLogAnalyticsFlujoTest {

    @Autowired
    private ConsultaLogs logs;

    @Test
    void conWorkspaceLaAppArrancaConLaConsultaDeAzure() {
        assertInstanceOf(ConsultaLogsAzure.class, logs);
    }
}
