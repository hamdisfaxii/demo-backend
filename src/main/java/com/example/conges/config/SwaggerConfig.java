package com.example.conges.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger/OpenAPI pour la documentation automatique de l'API
 * Accessible sur : http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestion des Congés - API REST")
                        .version("1.0.0")
                        .description("API REST pour la gestion des demandes de congés avec intégration Dolibarr, " +
                                "workflow BPM, IA et multi-pays support")
                        .contact(new Contact()
                                .name("Équipe Développement")
                                .email("dev@gestion-conges.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Serveur local"))
                
                .addServersItem(new Server()
                        .url("https://api.gestion-conges.local")
                        .description("Serveur production"));
    }
}
