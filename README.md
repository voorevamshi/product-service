# 🛒 Product Service

A **Spring Boot 4** microservice that exposes a secure REST API for product catalog data.
It acts as an **OAuth 2.0 Resource Server** — every request must carry a valid JWT token
issued by an external Authorization Server (e.g. [Keycloak Identity Provider](https://github.com/voorevamshi/Kubernetes/blob/main/workloads/deployments%20/KEYCLOAK_SETUP.md)). No token = no access.

---

## 📚 Documentation

| Doc | Description |
|-----|-------------|
| [🔐 Security Guide](docs/security.md) | ELI5 explanation of JWT, OAuth2, and how Spring Security protects this service |
| [📋 Files Explained](docs/files-explained.md) | Every class and file broken down line by line |
| [⚙️ Configuration](docs/configuration.md) | `application.yml` and Actuator endpoints explained |
| [🌐 API Reference](docs/api-reference.md) | REST endpoints, request/response examples, error codes |
| [🔮 Roadmap](docs/roadmap.md) | Suggested improvements and next steps |
| [❓ FAQ](docs/faq.md) | Security Q&A — stolen credentials, Bearer Token, JWT vs OAuth2, resource-server vs client |

---

## 🏗️ Architecture Overview

```
         ┌───────────────────┐
         │   Client / API    │
         │   Gateway / UI    │
         └────────┬──────────┘
                  │  Authorization: Bearer <JWT>
                  ▼
         ┌─────────────────────────────────────────┐
         │        product-service  (:8081)          │
         │                                         │
         │  ┌─────────────────────────────────┐   │
         │  │  Spring Security Filter Chain   │   │
         │  │  (verifies JWT signature/expiry) │   │
         │  └────────────────┬────────────────┘   │
         │                   ▼                     │
         │  ┌─────────────────────────────────┐   │
         │  │       ProductController          │   │
         │  │  GET /api/products               │   │
         │  │  GET /api/products/{id}          │   │
         │  └─────────────────────────────────┘   │
         └─────────────────────────────────────────┘
                  │ fetches public keys (JWKS) once
                  ▼
         ┌───────────────────┐
         │  Keycloak (:9000) │
         │  /realms/vmc-realm│
         └───────────────────┘
```

---

## 📦 Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 4.0.5 |
| Security | OAuth2 Resource Server (Spring Security) | BOM managed |
| Observability | Spring Boot Actuator | BOM managed |
| Boilerplate | Lombok | optional |
| Build | Maven | 3.x |

---

## 🚀 Quick Start

```bash
# Prerequisites: Java 21+, Maven, Keycloak running at localhost:9000

./mvnw spring-boot:run

# With a custom Auth Server:
AUTH_ISSUER_URI=http://my-keycloak:9000/realms/my-realm ./mvnw spring-boot:run
```

Service starts on **`http://localhost:8081`**

See [API Reference](docs/api-reference.md) for how to call the endpoints.

---

## 🗂️ Project Structure

```
product-service/
│
├── pom.xml
├── README.md
├── docs/                                    ← 📚 All detailed documentation
│   ├── security.md
│   ├── files-explained.md
│   ├── configuration.md
│   ├── api-reference.md
│   ├── roadmap.md
│   └── faq.md                             ← ❓ Security Q&A
│
└── src/main/
    ├── resources/
    │   └── application.yml
    └── java/com/vmc/product/
        ├── ProductServiceApplication.java   ← Entry point
        ├── Agent.java                       ← Practice POJO
        ├── ClaimCalcuation.java             ← Practice Streams demo
        ├── config/SecurityConfig.java       ← 🔐 Security rules
        ├── controller/ProductController.java
        ├── dto/ProductResponse.java
        └── exception/GlobalExceptionHandler.java
```

---

## 👤 Author

**Vamshi** — `com.vmc` (Vamshi's Micro [services] Codebases)
