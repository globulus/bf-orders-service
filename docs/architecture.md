# orders-service architecture

_Last reviewed: March 2025_

## Context

```
 web (React) ─┐                        ┌─> catalog-service  (product names and prices)
              ├─> orders-service ──────┤
 mobile ──────┘        │               └─> notification client (logs for now, SHOP-412)
                       v
                   PostgreSQL (database "orders")
```

orders-service owns orders and their lines. It does not own products: an order line
stores the catalog's `productId` together with a copy of the product name and unit price
taken when the order is placed. Later price or name changes in the catalog don't touch
existing orders.

## Layers

| Package | What's in it |
|---|---|
| `api` | REST controllers, request and response records, the exception handler (RFC 7807 problem details) |
| `service` | `OrderService`: transactions, pricing, state changes |
| `domain` | JPA entities `Order` and `OrderLine`, `OrderStatus` |
| `repository` | Spring Data repositories |
| `catalog` | `CatalogClient`, a `RestClient` against catalog-service |
| `notification` | the order-placed event and its listener |

## Order lifecycle

```
PLACED ──ship──> SHIPPED
   │
   └──cancel──> CANCELLED
```

- An order is created as `PLACED`.
- `SHIPPED` is set by the warehouse integration through `POST /api/v1/orders/{id}/ship`.
- A `PLACED` order can be cancelled. Shipped and cancelled orders can't (409).

## API

| Method | Path | |
|---|---|---|
| `POST` | `/api/v1/orders` | place an order; 201 with `Location` |
| `GET` | `/api/v1/orders?customerId=&status=&page=&size=` | newest first, `size` at most 100 |
| `GET` | `/api/v1/orders/{id}` | one order with its lines |
| `DELETE` | `/api/v1/orders/{id}` | cancel an order |
| `POST` | `/api/v1/orders/{id}/ship` | mark shipped (warehouse only) |

Errors are problem details: 400 for validation, 404 for an unknown order, 409 for a state
change that isn't allowed, 422 when a product is unknown, inactive or unpriced.

## Pricing

When an order is placed, `OrderService` asks catalog-service for each product
(`GET /api/products/{id}`) and uses its current price. All lines must share one currency;
the order takes the currency of its first line. If the catalog is down, placing an order
fails with a 500 rather than guessing a price. There is no retry yet.

## Notifications

After the transaction commits, `OrderConfirmationListener` sends an order confirmation
on a separate thread (`@Async`). A failure is logged and never fails the order.

## Persistence

Flyway owns the schema (`db/migration`). Hibernate only validates it
(`ddl-auto: validate`). Tests run the same migrations on H2 in PostgreSQL mode.

## Things we know we should do

- Idempotency key on `POST /api/v1/orders` (double-clicks create two orders).
- Retry and circuit breaker around `CatalogClient`.
- Real authentication; `customerId` is trusted from the request today.
