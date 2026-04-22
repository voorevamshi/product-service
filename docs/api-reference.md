# 🌐 API Reference

> [← Back to README](../README.md)

Complete reference for all REST endpoints exposed by the `product-service`.

---

## Base URL

```
http://localhost:8081
```

---

## Authentication

All endpoints (except Actuator) require a **Bearer JWT token** in the `Authorization` header.

```
Authorization: Bearer <your-jwt-token>
```

Without a valid token, all protected endpoints return:
```
HTTP 401 Unauthorized
```

---

## How to Get a Token (Local Dev)

```bash
curl -s -X POST http://localhost:9000/realms/vmc-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=<your-client-id>" \
  -d "client_secret=<your-client-secret>" \
  | jq -r '.access_token'
```

Copy the token and use it in all subsequent requests.

---

## Endpoints

### `GET /api/products`

Returns a list of all products.

| Detail | Value |
|--------|-------|
| URL | `GET /api/products` |
| Auth | ✅ JWT required |
| Response | `200 OK` — JSON array of products |

#### Example Request

```bash
curl -X GET http://localhost:8081/api/products \
  -H "Authorization: Bearer <your-token>"
```

#### Example Response — `200 OK`

```json
[
  {
    "id": "P100",
    "name": "Microservices Architecture",
    "description": "Advanced Guide",
    "price": 49.99
  },
  {
    "id": "P200",
    "name": "Spring Boot 4 In Action",
    "description": "Latest features",
    "price": 59.99
  }
]
```

---

### `GET /api/products/{id}`

Returns a single product by its ID.

| Detail | Value |
|--------|-------|
| URL | `GET /api/products/{id}` |
| Path param | `id` — the product ID (e.g., `P100`) |
| Auth | ✅ JWT required |
| Response | `200 OK` — single product JSON |

#### Example Request

```bash
curl -X GET http://localhost:8081/api/products/P100 \
  -H "Authorization: Bearer <your-token>"
```

#### Example Response — `200 OK`

```json
{
  "id": "P100",
  "name": "Mock Product",
  "description": "Description",
  "price": 100.00
}
```

> ⚠️ Currently returns **mock data** for any ID. A real database lookup is planned.

---

## Error Responses

### `401 Unauthorized`

Returned by Spring Security when:
- The `Authorization` header is missing
- The JWT is malformed
- The JWT signature is invalid
- The JWT is expired

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer
```

> No body is returned by Spring Security's default 401 handler.

---

### `500 Internal Server Error`

Returned by `GlobalExceptionHandler` when an unexpected exception occurs in any controller.

```json
{
  "error": "An unexpected error occurred",
  "message": "<exception message here>"
}
```

---

## Actuator Endpoints (No Auth)

These are **not** product API endpoints — they are for infrastructure health monitoring.

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Overall health status |
| `GET /actuator/health/liveness` | Kubernetes liveness probe |
| `GET /actuator/health/readiness` | Kubernetes readiness probe |
| `GET /actuator/info` | Application name and info |
| `GET /actuator/metrics` | JVM and HTTP metrics |

#### Example

```bash
curl http://localhost:8081/actuator/health

# Response:
{
  "status": "UP",
  "components": { ... }
}
```

---

## Quick Test Checklist

```bash
# 1. Confirm service is running
curl http://localhost:8081/actuator/health

# 2. Confirm 401 without token
curl http://localhost:8081/api/products

# 3. Get token from Keycloak
TOKEN=$(curl -s -X POST ... | jq -r '.access_token')

# 4. Get all products with token
curl http://localhost:8081/api/products -H "Authorization: Bearer $TOKEN"

# 5. Get product by ID
curl http://localhost:8081/api/products/P100 -H "Authorization: Bearer $TOKEN"
```

---

> [← Back to README](../README.md) | [Files Explained →](files-explained.md) | [Security →](security.md)
