# Catalog service guide

## Access boundaries

| Endpoint family | Access |
| --- | --- |
| `/api/v1/public/catalog/**` | Public customer read API |
| `/api/v1/catalog/**` | Authenticated restaurant management; service enforces restaurant membership |
| `/internal/v1/catalog/**` | Internal only; never call from frontend |

The generated `catalog-service.yaml` is the authoritative structural contract for menu, category, item, branch-item, option and image endpoints.

## Customer-facing catalogue model

- A `CatalogItem` is restaurant-level source data. Its `basePrice` is not the price a customer necessarily pays in a branch.
- A `BranchItem` connects an item to a branch and provides `sellingPrice`, optional original price, and branch-level availability. Customer UI should use the branch selling price and availability.
- Public catalogue responses are scoped by restaurant and branch. Do not infer that an item is saleable in a different branch merely because its catalog item exists.
- Menus/categories determine presentation and membership; option groups/values are additional item configuration, not independent purchasable catalog items.

Management authorization comes from restaurant membership roles (`OWNER`, `MANAGER`, `CATALOG_MANAGER`, etc.), not a frontend-supplied restaurant id.

## Internal rebuild endpoint

`POST /internal/v1/catalog/search-reindex` is ADMIN-only service-to-service work used by search rebuild. It emits current CatalogItem and BranchItem snapshots through the catalog transactional outbox. Browser clients must not call it.
