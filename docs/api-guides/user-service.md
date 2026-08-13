# User service guide

## Access

All endpoints below require an authenticated Keycloak Bearer token. The current user is derived from JWT subject; clients never send a `userId` to establish ownership.

| Method | Endpoint | Access |
| --- | --- | --- |
| GET | `/api/v1/users/me` | Authenticated |
| PATCH | `/api/v1/users/me` | Authenticated |
| GET | `/api/v1/users/me/addresses` | Authenticated |
| POST | `/api/v1/users/me/addresses` | Authenticated |
| PATCH | `/api/v1/users/me/addresses/{addressId}` | Authenticated owner |
| PATCH | `/api/v1/users/me/addresses/{addressId}/default` | Authenticated owner |
| DELETE | `/api/v1/users/me/addresses/{addressId}` | Authenticated owner |

Use the generated `user-service.yaml` for request and response schemas.

## Current profile

Authentication identity belongs to Keycloak; `/users/me` is the local business profile. Frontends should display business-friendly profile fields, not JWT subject or local UUIDs.

## Address semantics

- `PATCH` is partial: omitted or `null` fields are not updated by the current contract. Clearing an optional value is not a separate JSON Merge Patch capability.
- The first address for a user becomes default even when the client sends `isDefault=false`.
- Creating an address with `isDefault=true` clears the existing default in the same transaction. At most one address per user is default.
- Setting default uses the dedicated `/default` operation; merely selecting an address in a frontend shopping session must not call it.
- Deleting the default selects the oldest remaining address as replacement when one exists.
- `HOME`, `WORK`, and `OTHER` are the address label types. `OTHER` requires a non-blank `customLabel`; a custom label for HOME/WORK is normalized away.
- Latitude and longitude are optional today, but when supplied are validated to geographic ranges. They are not a manual frontend input in the current UX.
- Address list order is deterministic: default first, then creation time.

An address outside the authenticated user's ownership is treated as not found; clients must not infer or expose another user's address data.
