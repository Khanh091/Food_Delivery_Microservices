# Docker networking

## Quy tắc cốt lõi

| Nguồn gọi | Địa chỉ phải dùng |
| --- | --- |
| Browser, IDE hoặc công cụ chạy trên Windows host | `localhost:<host-port>` |
| Một container gọi một dịch vụ trong cùng Compose network | `<compose-service-name>:<container-port>` |
| Container gọi một tiến trình thật sự chạy ngoài Docker trên Windows host | `host.docker.internal:<port>` |

`localhost` bên trong container luôn là container hiện tại. Ngoại lệ hợp lệ là healthcheck tự kiểm tra chính container đó.

## Endpoint matrix local

| Component | Truy cập từ Windows host | Truy cập từ container cùng network |
| --- | --- | --- |
| Customer frontend (Vite) | `http://localhost:5173` | — |
| API Gateway | `http://localhost:8081` | `http://api-gateway:8081` |
| Keycloak | `http://localhost:8180` | `http://keycloak:8080` |
| Eureka | `http://localhost:8761` | `http://discovery-service:8761` |
| PostgreSQL | `localhost:${POSTGRES_PORT}` (mặc định Compose là `5433`; local `.env` có thể override) | `postgres:5432` |
| Kafka | `localhost:${KAFKA_PORT:-9094}` | `kafka:9092` |
| Elasticsearch | `http://localhost:9200` | `http://elasticsearch:9200` |
| User service | `http://localhost:8101` | `http://user-service:8101` |
| Restaurant service | `http://localhost:8102` | `http://restaurant-service:8102` |
| Catalog service | `http://localhost:8103` | `http://catalog-service:8103` |
| Search service | `http://localhost:8104` | `http://search-service:8104` |

Published service ports are for local development, diagnostics, and runtime OpenAPI export. Browser business traffic uses the Gateway, not `8101`–`8104` directly.

## Keycloak and JWT resource servers

Keycloak has two intentional addresses:

- Public issuer and browser identity: `http://localhost:8180/realms/food-delivery`
- Docker-internal JWK Set endpoint: `http://keycloak:8080/realms/food-delivery/protocol/openid-connect/certs`

The issuer remains public because it is the `iss` value in browser-issued JWTs and is also the Google callback host. Resource servers validate that issuer and the expected audience, while retrieving signing keys through Docker DNS. They must not use issuer discovery at `localhost:8180` from inside a container.

## Eureka

Compose clients use `http://discovery-service:8761/eureka/`. Each active service registers its Compose service name (`api-gateway`, `user-service`, `restaurant-service`, `catalog-service`, and `search-service`) so Gateway `lb://...` routes resolve to reachable container hosts.

## Infrastructure details

- PostgreSQL service clients use `jdbc:postgresql://postgres:5432/<database>`.
- Kafka has an internal listener `kafka:9092` and host listener `localhost:9094`; auto topic creation remains disabled.
- Elasticsearch service clients use `http://elasticsearch:9200`.
- `host.docker.internal` is not needed by the current Compose services. Add it only for a verified dependency that runs outside Docker.

## When `localhost` is correct

It is correct for browser/host access and for container self-healthchecks, such as Kafka checking `localhost:9092`, Elasticsearch checking `localhost:9200`, and an application checking its own actuator port. It is not a valid container-to-container hostname.
