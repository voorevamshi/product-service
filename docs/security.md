# 🔐 Security Guide

> [← Back to README](../README.md)

This doc explains **how this service is secured**, from the JWT basics all the way to
every line of `SecurityConfig.java`. Written ELI5 style — no prior OAuth knowledge needed.

---

## Table of Contents

1. [The Big Picture — ELI5](#the-big-picture--eli5)
2. [What is a JWT?](#what-is-a-jwt)
3. [What is an Authorization Server?](#what-is-an-authorization-server)
4. [What is an OAuth 2.0 Resource Server?](#what-is-an-oauth-20-resource-server)
5. [How Spring Validates the JWT — Step by Step](#how-spring-validates-the-jwt--step-by-step)
6. [SecurityConfig.java — Line by Line](#securityconfigjava--line-by-line)
7. [application.yml — Security Section](#applicationyml--security-section)

---

## The Big Picture — ELI5

> Imagine a **nightclub** 🎉.
> - The **bouncer at the door** = Authorization Server (Keycloak). He checks your ID and gives you a wristband.
> - The **wristband** = JWT Token. Proves you're allowed in.
> - The **room inside the club** = this `product-service`. It doesn't check IDs — it only checks wristbands.
>
> **No wristband = no entry.** The room never hands out wristbands — that's the bouncer's job.

This service **never stores passwords** and **never issues tokens**. It only validates them.

---

## What is a JWT?

A JWT (JSON Web Token) is a tiny digital ticket with three parts separated by dots:

```
eyJhbGc...  .  eyJ1c2VyI...  .  SflKxwRJ...
   HEADER         PAYLOAD        SIGNATURE
```

| Part | What It Is | ELI5 |
|------|-----------|------|
| **Header** | Algorithm used to sign the token | "This wristband uses invisible ink" |
| **Payload** | Claims: who you are, roles, expiry time | "Your name, VIP status, expires at midnight" |
| **Signature** | Cryptographic proof it wasn't tampered | "The club's stamp that cannot be faked" |

### What's Inside the Payload?

```json
{
  "sub":   "user-id-123",
  "iss":   "http://localhost:9000/realms/vmc-realm",
  "exp":   1714000000,
  "roles": ["user", "admin"]
}
```

| Claim | Meaning |
|-------|---------|
| `sub` | Subject — who this token belongs to |
| `iss` | Issuer — which Auth Server created this token |
| `exp` | Expiry timestamp — token is invalid after this |
| `roles` | What the user is allowed to do |

---

## What is an Authorization Server?

It is a **separate service** (here: Keycloak at `http://localhost:9000/realms/vmc-realm`) that:

1. Accepts a user's credentials (username + password / client credentials)
2. Verifies who they are
3. Signs and returns a JWT token using its **private key**

```
Client ──── username/password ──►  Keycloak
Client ◄─── JWT (signed token) ──  Keycloak
Client ──── JWT in header ───────► product-service
```

> 🔑 Keycloak keeps its **private key** secret. It publishes the corresponding **public key** at a JWKS endpoint.
> Our service downloads this public key and uses it to verify every incoming JWT — without ever talking to Keycloak again per request.

---

## What is an OAuth 2.0 Resource Server?

OAuth 2.0 defines roles in the flow:

| Role | In this project | ELI5 |
|------|----------------|------|
| **Resource Owner** | End user | You |
| **Authorization Server** | Keycloak at `:9000` | Ticket office |
| **Resource Server** | **This service** at `:8081` | The museum |
| **Client** | Frontend / API Gateway | Your ticket scanner app |

> ELI5: The museum doesn't sell tickets. You buy a ticket from the ticket office (Keycloak).
> At the museum door, the guard (Spring Security) scans your ticket and lets you in only if it's genuine.

---

## How Spring Validates the JWT — Step by Step

```
  Incoming Request: GET /api/products
  Authorization: Bearer eyJhbGc...
         │
         ▼
  ┌─────────────────────────────────────────────┐
  │         Spring Security Filter Chain        │
  │                                             │
  │  Step 1: Extract token from the             │
  │          "Authorization: Bearer ..." header │
  │                                             │
  │  Step 2: Fetch public keys (JWKS) from      │
  │          issuer-uri  (done ONCE, cached)    │
  │                                             │
  │  Step 3: Verify the JWT Signature           │
  │          using the public key  ✅ / ❌      │
  │                                             │
  │  Step 4: Check token is not expired         │
  │                                             │
  │  Step 5: Check `iss` matches issuer-uri     │
  └──────────────┬──────────────────────────────┘
                 │
         Valid? ─┤
                 │
        ✅ YES ──▼──────► Controller runs  →  200 OK
                 │
        ❌ NO  ──▼──────► Request blocked  →  401 Unauthorized
```

**Spring does ALL of steps 1–5 automatically** because of this one line:
```java
.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))
```

You write **zero validation code**. Spring handles everything.

---

## `SecurityConfig.java` — Line by Line

```java
package com.vmc.product.config;

@Configuration              // ① Tells Spring: "This class registers beans"
public class SecurityConfig {

    @Bean                   // ② Register filterChain as a Spring-managed bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
          // ③ Disable CSRF protection
          .csrf(csrf -> csrf.disable())

          // ④ Define authorization rules
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/actuator/**").permitAll()  // ⑤ Health checks are PUBLIC
              .anyRequest().authenticated()                 // ⑥ Everything else needs JWT
          )

          // ⑦ Enable JWT validation
          .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}));

        return http.build();
    }
}
```

### ③ Why disable CSRF?

> ELI5: **CSRF attacks** trick your browser into secretly sending requests using your cookies.
> JWT tokens live in the `Authorization` **header**, not cookies.
> A malicious website cannot steal your header — so CSRF attacks simply don't apply here.
> Disabling CSRF on stateless REST APIs is the **correct and safe** thing to do.

### ⑤ Why permit `/actuator/**`?

> ELI5: Kubernetes pokes `/actuator/health` every few seconds to check "is this pod alive?".
> If it required a JWT, Kubernetes would need its own token — which is unnecessary complexity.
> Health endpoints are intentionally left **open to the world** but expose **no sensitive data**.

### ⑥ `.anyRequest().authenticated()`

Every other URL (including `/api/products`) requires a valid JWT.  
No token or an expired/invalid token → Spring returns `401 Unauthorized` before the controller even runs.

### ⑦ `.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))`

This triggers Spring's auto-configuration to:
1. Read `issuer-uri` from `application.yml`
2. Fetch public keys from `{issuer-uri}/.well-known/openid-configuration` → `jwks_uri`
3. Cache those keys in memory
4. Validate every incoming JWT against those keys automatically

---

## `application.yml` — Security Section

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${AUTH_ISSUER_URI:http://localhost:9000/realms/vmc-realm}
```

| Key | What it does |
|-----|-------------|
| `issuer-uri` | URL of the Auth Server realm. Spring auto-discovers the JWKS endpoint from here. |
| `${AUTH_ISSUER_URI:...}` | Reads from an environment variable. Falls back to the localhost default if not set. |

**In Kubernetes**, you set `AUTH_ISSUER_URI` to the **internal ClusterIP** of Keycloak,
so pod-to-pod traffic never leaves the cluster network.

---

> [← Back to README](../README.md) | [Files Explained →](files-explained.md) | [Configuration →](configuration.md)
