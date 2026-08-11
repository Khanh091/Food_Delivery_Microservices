# Restaurant Service

`restaurant-service` manages partner applications, application documents,
restaurants, branches, operating hours, members, and bank accounts.

## Document storage

Document upload uses the `FileStorageService` strategy. Cloudinary is the
default provider:

```text
STORAGE_PROVIDER=CLOUDINARY
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_BASE_FOLDER=food-delivery/restaurant-documents
```

Cloudinary credentials are required only when an upload or delete operation is
performed. They must be provided through environment variables and must not be
committed.

To use AWS S3, MinIO, or another S3-compatible service:

```text
STORAGE_PROVIDER=S3
S3_BUCKET=
S3_REGION=
S3_ENDPOINT=
S3_ACCESS_KEY=
S3_SECRET_KEY=
S3_PATH_STYLE_ACCESS=false
S3_BASE_FOLDER=restaurant-documents
```

Leave `S3_ENDPOINT` empty for AWS S3. Set an endpoint and usually enable
path-style access for MinIO.

The default maximum file size is 10 MB. Supported content types are:

- `image/jpeg`
- `image/png`
- `application/pdf`

Configure the byte limit with `STORAGE_MAX_FILE_SIZE`.

## Upload endpoint

```http
POST /api/v1/restaurant-applications/{applicationId}/documents
Content-Type: multipart/form-data
```

The request contains a `file` part and a JSON `metadata` part:

```json
{
  "documentType": "BUSINESS_LICENSE",
  "documentNumber": "LICENSE-123",
  "issuedAt": "2026-01-01",
  "expiresAt": "2027-01-01"
}
```

The database stores the provider, storage key, URL, sanitized original
filename, MIME type, and file size. The internal storage key is not exposed in
the public document response.

## Formatting and build

Run from this directory:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd clean package
```
