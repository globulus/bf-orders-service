package com.shopfront.orders.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    private final RestClient restClient;

    public CatalogClient(@Qualifier("catalogRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Looks a product up by its catalog ID. Empty when the catalog answers 404; any other
     * error propagates, so a catalog outage fails the order instead of pricing it at zero.
     */
    public Optional<CatalogProduct> findProduct(Long productId) {
        try {
            CatalogProduct product = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(CatalogProduct.class);
            return Optional.ofNullable(product);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.debug("Product {} not found in catalog", productId);
                return Optional.empty();
            }
            throw e;
        }
    }
}
