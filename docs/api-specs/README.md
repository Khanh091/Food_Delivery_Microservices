# API specification snapshots

The `*.yaml` files in this directory are generated snapshots of the running Spring services. They are not hand-maintained API definitions.

## Source of truth

Spring MVC mappings, Jackson serialization and Bean Validation on the existing controllers and DTOs are scanned by springdoc at runtime:

```text
running service -> /v3/api-docs.yaml -> this directory
```

This keeps OpenAPI documentation separate from business Java source. Do not add Swagger annotations to controllers or DTOs just to improve a description.

## Refresh a snapshot

Start the relevant services, obtain a short-lived development Bearer token, then run from the repository root:

```powershell
$env:OPENAPI_ACCESS_TOKEN = '<short-lived-token>'
.\docs\tooling\api-specs\export-api-specs.ps1
```

To refresh one service only:

```powershell
.\docs\tooling\api-specs\export-api-specs.ps1 search-service
```

The documentation endpoints use the service security policy, so the exporter does not bypass authentication. Never commit a token or put it in the script.

This project intentionally uses the springdoc **API** starter, not the Swagger UI starter. The developer contract URLs are `/v3/api-docs` and `/v3/api-docs.yaml`; `/swagger-ui/index.html` is not packaged or exposed.

Direct ports are only for local development, health checks, and exporting documentation. Frontend business requests go through API Gateway (`:8081`), not ports `8101`–`8104`.

After an API change, regenerate the affected snapshot, review the YAML diff, and commit it with the backend change. Validate the file as YAML before commit.
