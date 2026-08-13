# API usage guides

`docs/api-specs` answers the machine-readable part of an API contract: paths, HTTP methods, parameters, request/response schemas, enums and validation constraints.

These guides contain behavior that generated OpenAPI cannot reliably express: authorization, workflow/state rules, PATCH semantics, visibility rules and frontend usage notes.

Recommended frontend workflow:

1. Read the relevant generated YAML snapshot.
2. Read the matching guide for business semantics and access rules.
3. Audit backend source only when documentation and runtime disagree; runtime remains the source of truth.

All public frontend traffic must use API Gateway at `http://localhost:8081` in local development. An `/internal/**` endpoint is for service-to-service work and must never be called by the browser.
