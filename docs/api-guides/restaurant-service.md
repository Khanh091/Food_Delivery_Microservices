# Restaurant service guide

## Partner-to-restaurant workflow

Restaurant creation is a workflow, not a direct client-side database operation:

```text
Partner application -> document upload/update -> submit
-> admin review/verification -> approve -> restaurant created
-> restaurant and branch lifecycle operations
```

The exact request bodies, validation, document endpoints, and lifecycle paths are in `restaurant-service.yaml`. Follow the exposed workflow endpoints rather than attempting to create, approve, or activate data by SQL.

## Access boundaries

| Endpoint family | Typical access |
| --- | --- |
| `/api/v1/partner-applications/**` | Applicant/owner workflow; admin review actions are role restricted |
| `/api/v1/restaurants/me` and restaurant management | Authenticated restaurant member/owner as enforced by service |
| Restaurant status suspension | ADMIN or SUPPORT |
| `/api/v1/restaurants/{restaurantId}/members/**` | Authorized restaurant management |
| `/internal/v1/restaurants/**` | Internal only, ADMIN service-to-service operation |
| `GET /api/v1/public/restaurants/{restaurantId}/branches/{branchId}` | Public customer branch detail |

Role enforcement and membership checks in the service are the source of truth; the table is a usage guide, not a replacement for runtime authorization.

## Public restaurant branch detail

`GET /api/v1/public/restaurants/{restaurantId}/branches/{branchId}` is the customer-facing
read endpoint used after a branch result is selected from Search. It returns an
`ApiResponse` whose data contains only the public restaurant/branch presentation data:
restaurant and branch names, optional restaurant description/logo/cover URL, branch contact
and address, `acceptingOrders`, and configured weekly business hours.

The endpoint is public, but only returns a branch when both its Restaurant and
RestaurantBranch are `ACTIVE`. A missing, mismatched, or non-public branch is represented as
the service's normal `BRANCH_NOT_FOUND` 404 response. It intentionally does not expose members,
owners, application records, banking, tax data, lifecycle history, audit fields, or coordinates.

The full menu remains owned by Catalog Service. Customer Web must call the separate public
Catalog endpoint documented in `catalog-service.md`; Restaurant Service does not query Catalog's
database or aggregate its menu into this response.

## Restaurant and branch lifecycle

Restaurant and branch data customer-facing enough for search propagate through the restaurant transactional outbox. The search projection is eventually consistent; a successful management mutation does not guarantee that the search index has already caught up in the same HTTP response.

`POST /internal/v1/restaurants/search-reindex` is the ADMIN-only snapshot endpoint used by search-service during a rebuild. It must never be called by the frontend.
