package com.shopfront.orders.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogClientTest {

    private MockRestServiceServer server;
    private CatalogClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://catalog");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CatalogClient(builder.build());
    }

    @Test
    void readsProductAndIgnoresFieldsOrdersDoesNotUse() {
        server.expect(requestTo("http://catalog/api/products/42"))
                .andRespond(withSuccess("""
                        {"id": 42, "sku": "BEAN-1KG", "name": "Espresso beans 1kg", "description": "Dark roast",
                         "categoryId": 3, "active": true, "price": {"amount": 18.90, "currency": "EUR"}}
                        """, MediaType.APPLICATION_JSON));

        CatalogProduct product = client.findProduct(42L).orElseThrow();

        assertThat(product.name()).isEqualTo("Espresso beans 1kg");
        assertThat(product.price().amount()).isEqualByComparingTo("18.90");
        assertThat(product.active()).isTrue();
    }

    @Test
    void notFoundIsEmpty() {
        server.expect(requestTo("http://catalog/api/products/7")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findProduct(7L)).isEmpty();
    }

    @Test
    void serverErrorPropagates() {
        server.expect(requestTo("http://catalog/api/products/7")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.findProduct(7L)).isInstanceOf(HttpServerErrorException.class);
    }
}
