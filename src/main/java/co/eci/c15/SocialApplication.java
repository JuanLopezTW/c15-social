package co.eci.c15;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio social del EscapeRoom C15: perfil (auth), chat y ranking/estadisticas. Sale del monolito
 * (repo ScapeRoomC15) con los mismos paquetes; lo comun a todos los servicios esta en {@code plataforma}.
 */
@SpringBootApplication
public class SocialApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }
}
