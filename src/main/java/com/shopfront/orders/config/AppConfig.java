package com.shopfront.orders.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public OpenAPI ordersOpenApi(@Value("${app.api-version:1.4.0}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("Orders API")
                        .description("Placing, listing and cancelling customer orders.")
                        .version(version))
                .servers(List.of(new Server().url("/").description("Relative to the host serving the API")));
    }
}
