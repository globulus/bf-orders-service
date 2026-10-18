# Known issues

Bugs we know about and haven't fixed yet. Each one has a test tagged `known-bug` that
fails until it's fixed. CI skips that tag (`./gradlew build -PexcludeTags=known-bug`);
a local `./gradlew test` runs it. When you fix one, remove the tag and the entry here.

## Duplicate product lines lose quantity

**Reported:** support ticket, customer ordered 2 + 3 bags of the same coffee from two
places in the web cart and was charged for 3.

**What happens:** when a create request contains the same `productId` on more than one
line, `OrderService.createOrder` collapses them into one line, but keeps only the
quantity of the last line instead of adding them up. The total is wrong too.

**Expected:** one line per product with the quantities summed (2 + 3 = 5).

**Test:** `OrderServiceTest.createOrder_sumsQuantitiesOfDuplicateProductLines`

**Workaround:** the web cart merges lines before sending, so the web is mostly fine. The
mobile app doesn't.
