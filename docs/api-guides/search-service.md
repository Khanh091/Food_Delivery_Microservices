# Search service guide

## Public endpoints

| Method | Endpoint | Access |
| --- | --- | --- |
| GET | `/api/v1/search` | Public |
| GET | `/api/v1/search/items` | Public |

`GET /api/v1/search` is the global lexical search. `q` is required; `page` and `size` paginate **branch/store results**, not raw item documents.

The result unit is `restaurantId + branchId`. One restaurant may therefore appear in several rows—one for each matching active branch. `matchingItems[]` contains only matching items offered by that particular branch and never causes duplicate branch rows.

The global query matches restaurant/branch text and catalog item name or description. Only active restaurant and branch projections are returned. Item candidates also require an active catalog item and an available branch item. `acceptingOrders=false` does not hide an otherwise visible branch; it is returned so the client can present its current ordering state.

`GET /api/v1/search/items` is the existing branch-scoped item search. It requires `branchId`; use its generated schema for optional filters and pagination.

## Administrative rebuilds

| Method | Endpoint | Access |
| --- | --- | --- |
| POST | `/api/v1/search/admin/catalog-items/rebuild` | ADMIN |
| POST | `/api/v1/search/admin/restaurants/rebuild` | ADMIN |

Both return `202 Accepted`: snapshot enqueueing has been accepted, while Elasticsearch convergence continues asynchronously through the outbox/Kafka pipeline. They are infrastructure operations, not frontend customer actions.
