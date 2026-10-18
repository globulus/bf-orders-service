package com.shopfront.orders.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * The parts of catalog-service's ProductResponse that orders need. Keep this in step with
 * GET /api/products/{id} in catalog-service by hand; there is no shared client library.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogProduct(Long id, String sku, String name, boolean active, Price price) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Price(BigDecimal amount, String currency) {
    }
}
