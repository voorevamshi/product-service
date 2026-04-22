# ⚙️ Configuration Guide

> [← Back to README](../README.md)

Everything you need to know about `application.yml` — every property explained, and how
Spring Boot Actuator is configured for Kubernetes health checks.

---

## `application.yml` — Full File

```yaml
server:
  port: 8081

spring:
  application:
    name: product-service
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${AUTH_ISSUER_URI:http://localhost:9000/realms/vmc-realm}

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
```

---

## Server Configuration

```yaml
server:
  port: 8081
```

| Property | Value | Why |
|----------|-------|-----|
| `port` | `8081` | Port `8080` is conventionally reserved for an API Gateway or frontend. This service runs on `8081` to avoid conflicts. |

---

## Spring Application Identity

```yaml
spring:
  application:
    name: product-service
```

This name is used by:
- **Spring Boot Actuator** → appears in `/actuator/info`
- **Service discovery tools** (Eureka, Consul, Kubernetes) → services refer to each other by this name
- **Distributed tracing** (Sleuth, Zipkin) → log traces include this name

---

## Security / OAuth2 Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${AUTH_ISSUER_URI:http://localhost:9000/realms/vmc-realm}
```

| Property | Explanation |
|----------|-------------|
| `issuer-uri` | The URL of the Authorization Server realm. Spring uses this to auto-discover the public keys (JWKS endpoint) for JWT validation. |
| `${AUTH_ISSUER_URI:...}` | **Environment variable substitution** with a default fallback. If the env var `AUTH_ISSUER_URI` is set, it uses that. Otherwise, it falls back to the localhost Keycloak URL. |

### How `issuer-uri` works internally

Spring Boot performs **OIDC Discovery** at startup:

```
issuer-uri + "/.well-known/openid-configuration"
    → Returns a JSON doc with a "jwks_uri" field
    → Spring fetches the public keys from that "jwks_uri"
    → Keys are cached in memory
    → All incoming JWTs are verified using these public keys
```

### Environment Variable in Different Environments

| Environment | `AUTH_ISSUER_URI` value |
|-------------|------------------------|
| Local dev | *(not set)* → uses `http://localhost:9000/realms/vmc-realm` |
| Docker Compose | `http://keycloak:9000/realms/vmc-realm` (container name) |
| Kubernetes | `http://keycloak-svc.auth-ns.svc.cluster.local:9000/realms/vmc-realm` (K8s internal DNS) |

---

## Actuator / Observability Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics   # Only these 3 are reachable via HTTP
  endpoint:
    health:
      show-details: always               # Show detailed component health, not just UP/DOWN
      probes:
        enabled: true                    # Enable K8s liveness & readiness endpoints
```

### Why limit to only 3 endpoints?

By default, Spring Actuator has many endpoints (env, beans, mappings, loggers, etc.).
Exposing all of them to the internet is a **security risk** — they might leak configuration
details, class names, or sensitive environment variables.

Only exposing `health`, `info`, `metrics` is the **minimum needed** for Kubernetes and monitoring.

---

## Actuator Endpoints Reference

| Endpoint | URL | Auth Required | Purpose |
|----------|-----|--------------|---------|
| Health | `/actuator/health` | ❌ Public | Overall status (`UP`/`DOWN`) |
| Liveness | `/actuator/health/liveness` | ❌ Public | K8s liveness probe |
| Readiness | `/actuator/health/readiness` | ❌ Public | K8s readiness probe |
| Info | `/actuator/info` | ❌ Public | App name, version |
| Metrics | `/actuator/metrics` | ❌ Public | JVM stats, HTTP timings |

### Sample Health Response

```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 120000000000,
        "free":  80000000000,
        "threshold": 10485760
      }
    },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

---

## Kubernetes Probes — ELI5

Kubernetes runs your app in a **Pod**. It needs to know:

| Question | Probe | URL |
|----------|-------|-----|
| "Is the JVM process alive? Should I restart it?" | **Liveness** | `/actuator/health/liveness` |
| "Is the app ready to handle traffic?" | **Readiness** | `/actuator/health/readiness` |

```yaml
# Typical K8s deployment probe config (for reference)
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 10
  periodSeconds: 5
```

> ELI5: The liveness probe asks "are you still breathing?". The readiness probe asks "are you ready to work?".
> A pod can be alive but not ready (e.g., still loading data at startup). Kubernetes waits until it's ready before sending it traffic.

`probes.enabled: true` in `application.yml` is what creates these two sub-endpoints automatically.

---

> [← Back to README](../README.md) | [Security →](security.md) | [API Reference →](api-reference.md)
