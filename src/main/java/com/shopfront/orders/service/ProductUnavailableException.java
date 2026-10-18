package com.shopfront.orders.service;

/**
 * Thrown when an order line points at a product the catalog doesn't know, has deactivated,
 * or has no price for.
 */
public class ProductUnavailableException extends RuntimeException {

    private final Long productId;

    public ProductUnavailableException(Long productId, String why) {
        super("Product " + productId + " " + why);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
