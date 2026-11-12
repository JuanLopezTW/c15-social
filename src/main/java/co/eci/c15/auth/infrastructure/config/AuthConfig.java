package co.eci.c15.auth.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "co.eci.c15.auth.infrastructure.persistence")
@EntityScan(basePackages = "co.eci.c15.auth.infrastructure.persistence")
public class AuthConfig {
}
