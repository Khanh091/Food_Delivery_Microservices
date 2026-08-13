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

Role enforcement and membership checks in the service are the source of truth; the table is a usage guide, not a replacement for runtime authorization.

## Restaurant and branch lifecycle

Restaurant and branch data customer-facing enough for search propagate through the restaurant transactional outbox. The search projection is eventually consistent; a successful management mutation does not guarantee that the search index has already caught up in the same HTTP response.

`POST /internal/v1/restaurants/search-reindex` is the ADMIN-only snapshot endpoint used by search-service during a rebuild. It must never be called by the frontend.
