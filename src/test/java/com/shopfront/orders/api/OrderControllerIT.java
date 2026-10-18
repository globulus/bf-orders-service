package com.shopfront.orders.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopfront.orders.catalog.CatalogClient;
import com.shopfront.orders.catalog.CatalogProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CatalogClient catalogClient;

    @BeforeEach
    void catalogKnowsEveryProduct() {
        when(catalogClient.findProduct(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Optional.of(new CatalogProduct(id, "SKU-" + id, "Product " + id, true,
                    new CatalogProduct.Price(new BigDecimal("10.00"), "EUR")));
        });
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "cust-it-1", "lines": [{"productId": 1, "quantity": 2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/v1/orders/\\d+")))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(20.00))
                .andExpect(jsonPath("$.lines[0].productName").value("Product 1"));
    }

    @Test
    void createWithoutLinesIs400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "cust-it-1", "lines": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithZeroQuantityIs400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "cust-it-1", "lines": [{"productId": 1, "quantity": 0}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithUnknownProductIs422() throws Exception {
        when(catalogClient.findProduct(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "cust-it-1", "lines": [{"productId": 999, "quantity": 1}]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.productId").value(999));
    }

    @Test
    void getUnknownOrderIs404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", 987654))
                .andExpect(status().isNotFound());
    }

    @Test
    void listIsPagedAndFilteredByCustomer() throws Exception {
        String customer = "cust-" + UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            placeOrder(customer, i + 1);
        }
        placeOrder("someone-else", 1);

        mockMvc.perform(get("/api/v1/orders").param("customerId", customer).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0));

        mockMvc.perform(get("/api/v1/orders").param("customerId", customer).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void cancelTwiceIs409() throws Exception {
        long id = placeOrder("cust-it-cancel", 1);

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Ordered by mistake"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Ordered by mistake"));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", id))
                .andExpect(status().isConflict());
    }

    @Test
    void shippedOrderCannotBeCancelled() throws Exception {
        long id = placeOrder("cust-it-ship", 1);
        mockMvc.perform(post("/api/v1/orders/{id}/ship", id)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", id))
                .andExpect(status().isConflict());
    }

    private long placeOrder(String customerId, int quantity) throws Exception {
        String body = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\": \"" + customerId + "\", \"lines\": [{\"productId\": 1, \"quantity\": "
                                + quantity + "}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("id").isNumber()).isTrue();
        return json.get("id").asLong();
    }
}
