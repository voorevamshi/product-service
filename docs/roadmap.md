# 🔮 Roadmap & Improvements

> [← Back to README](../README.md)

Suggested next steps and known issues for the `product-service`.

---

## 🐛 Known Issues / Technical Debt

| # | Issue | File | Fix |
|---|-------|------|-----|
| 1 | Typo: `ClaimCalcuation` | `ClaimCalcuation.java` | Rename to `ClaimCalculation` |
| 2 | Typo: `cliamAount` | `Agent.java` | Rename to `claimAmount` |
| 3 | `Agent` and `ClaimCalcuation` are practice files in the wrong package | root package | Move to a separate `practice` module or remove |
| 4 | No unit or integration tests | `src/test/` | Empty test directory — needs tests |

---

## 🚀 Feature Roadmap

### Phase 1 — Foundation

- [ ] **Connect to a real database**
  - Add `spring-boot-starter-data-jpa` + PostgreSQL/MySQL driver
  - Create a `Product` JPA Entity
  - Create a `ProductRepository extends JpaRepository<Product, String>`
  - Create a `ProductService` layer (separate business logic from controller)

- [ ] **Add a Service Layer**
  ```
  ProductController → ProductService → ProductRepository → Database
  ```
  Right now the controller does everything. Separating concerns makes code testable and maintainable.

- [ ] **Write Tests**
  - `@WebMvcTest(ProductController.class)` — unit test the controller
  - `@SpringBootTest` — integration test the full app with a test JWT

### Phase 2 — Security Enhancements

- [ ] **Role-Based Access Control (RBAC)**
  ```java
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> deleteProduct(@PathVariable String id) { ... }
  ```
  Different endpoints require different roles from the JWT.

- [ ] **Extract JWT Claims**
  ```java
  // Get the logged-in user's info from the token inside a controller:
  @GetMapping("/me")
  public String whoAmI(JwtAuthenticationToken token) {
      return token.getToken().getClaimAsString("email");
  }
  ```

- [ ] **Audit Logging**
  Log `user`, `action`, and `timestamp` for every state-changing operation.

### Phase 3 — Production Readiness

- [ ] **Dockerization**
  ```dockerfile
  FROM eclipse-temurin:21-jre
  COPY target/product-service-*.jar app.jar
  ENTRYPOINT ["java", "-jar", "/app.jar"]
  ```

- [ ] **Docker Compose**
  ```yaml
  services:
    product-service:
      build: .
      ports: ["8081:8081"]
      environment:
        AUTH_ISSUER_URI: http://keycloak:9000/realms/vmc-realm
    keycloak:
      image: quay.io/keycloak/keycloak:latest
      ports: ["9000:8080"]
  ```

- [ ] **Kubernetes Manifests**
  - `Deployment` with liveness/readiness probes pointing to `/actuator/health/*`
  - `Service` (ClusterIP)
  - `ConfigMap` for non-sensitive config
  - `Secret` for client credentials

- [ ] **Input Validation**
  ```java
  // Add @Valid to request bodies + use Bean Validation annotations
  public record CreateProductRequest(
      @NotBlank String name,
      @DecimalMin("0.01") BigDecimal price
  ) {}
  ```

- [ ] **Pagination**
  ```java
  // For large product catalogs
  Page<ProductResponse> getAllProducts(Pageable pageable);
  ```

### Phase 4 — Observability

- [ ] **Structured Logging** — Use `logback` with JSON format for log aggregation (ELK/Loki)
- [ ] **Distributed Tracing** — Add `spring-boot-starter-actuator` + Micrometer tracing with a Zipkin/Grafana Tempo backend
- [ ] **Custom Metrics** — Expose business metrics (e.g., product view count) via `MeterRegistry`

---

## 🏗️ Target Architecture (After Roadmap)

```
Client
  │
  ▼
API Gateway (port 8080)
  │   - Rate limiting
  │   - Routing
  │
  ├──► product-service (port 8081)   ← This service
  │        │
  │        └──► PostgreSQL
  │
  ├──► order-service (port 8082)
  │
  └──► user-service (port 8083)
            │
            └──► Keycloak (Auth Server, port 9000)
```

---

> [← Back to README](../README.md) | [API Reference →](api-reference.md) | [Security →](security.md)
