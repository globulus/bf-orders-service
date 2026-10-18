package com.shopfront.orders.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "catalog")
public record CatalogProperties(
        @DefaultValue("http://localhost:8081") String baseUrl,
        @DefaultValue("2s") Duration timeout) {
}
