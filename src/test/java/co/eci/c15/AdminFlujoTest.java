package co.eci.c15;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El admin contra la app ensamblada: fuera de Azure no hay logs centralizados y lo dice con un 503. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminFlujoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sinLogsCentralizadosElAdminRecibe503ConElMotivo() throws Exception {
        mvc.perform(get("/api/admin/tablero/negocio"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", containsString("LOG_ANALYTICS_WORKSPACE")));
        mvc.perform(get("/api/admin/tablero/tecnico").param("minutos", "30"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void unCodigoInvalidoEs400() throws Exception {
        mvc.perform(get("/api/admin/errores/no-es-hex"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("hexadecimales")));
    }

    @Test
    void conUnCodigoValidoSeBuscaEnLosLogs() throws Exception {
        mvc.perform(get("/api/admin/errores/abcd1234").param("dias", "2"))
                .andExpect(status().isServiceUnavailable());
    }
}
