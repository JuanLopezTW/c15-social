package co.eci.c15.plataforma.observabilidad;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class ObservabilidadConfig {

    static {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                new Slf4jThreadLocalAccessor(ContextoLog.USER_ID, ContextoLog.SALA_ID, ContextoLog.MATCH_ID));
    }

    /** Lo programado (cuenta regresiva, reto, gracia del anfitrion) conserva el traceId y el contexto de quien lo pidio. */
    @Bean
    static BeanPostProcessor contextoEnTareasProgramadas() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof ThreadPoolTaskScheduler scheduler) {
                    scheduler.setTaskDecorator(new ContextPropagatingTaskDecorator());
                }
                return bean;
            }
        };
    }


    /** Conecta el appender de logback-spring.xml con el exportador OTLP. */
    @Bean
    InitializingBean instalarAppenderOtlp(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
