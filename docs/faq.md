# ❓ FAQ — Security Questions

> [← Back to README](../README.md)

Common questions about how OAuth2, JWT, and Spring Security work in this project — explained clearly with ELI5 analogies.

---

## Table of Contents

1. [Q1 — If OAuth2 credentials are stolen, can a hacker always access the API?](#q1--if-oauth2-credentials-are-stolen-can-a-hacker-always-access-the-api)
2. [Q2 — `oauth2-resource-server` vs `oauth2-client` — what's the difference?](#q2--oauth2-resource-server-vs-oauth2-client--whats-the-difference)
3. [Q3 — Is the token a Bearer Token? Can Keycloak give Access + Refresh Tokens?](#q3--is-the-token-a-bearer-token-can-keycloak-give-access--refresh-tokens)
4. [Q4 — JWT vs OAuth2 — what's the difference? Why do we need both?](#q4--jwt-vs-oauth2--whats-the-difference-why-do-we-need-both)

---

## Q1 — If OAuth2 credentials are stolen, can a hacker always access the API?

**Short answer: YES — but only for a limited window, and it depends on the Grant Type.**

### What each stolen piece gives the hacker:

| Stolen Info | What the hacker can do with it |
|-------------|-------------------------------|
| `client_id` alone | Nothing — it's public anyway |
| `client_id` + `client_secret` | **Get fresh access tokens using `client_credentials` flow — indefinitely** |
| `Auth URL` + `Token URL` | Just endpoint addresses — not dangerous alone |
| `Callback URL` | Used in `authorization_code` flow — helps intercept auth codes |

### The dangerous scenario — `client_credentials` grant

```
Hacker has: client_id + client_secret
─────────────────────────────────────
Hacker runs:
  POST /realms/vmc-realm/protocol/openid-connect/token
       grant_type=client_credentials
       client_id=vmc-client
       client_secret=<stolen>

Keycloak says: "Valid credentials → here is your token ✅"

Hacker calls:
  GET /api/products
  Authorization: Bearer <token>

Product-service says: "Valid JWT → 200 OK ✅"
```

> **Yes — they can keep doing this forever** until you rotate or revoke the client secret.

### The `authorization_code` flow is safer

With `authorization_code`, the hacker also needs to intercept the **one-time authorization code** that Keycloak sends back to the callback URL. Stealing just the client credentials is not enough — they would also need to impersonate a real user login.

### How to protect against credential theft:

| Protection | What it does |
|------------|-------------|
| **Short token expiry** (e.g., 5 min) | Even if a token is stolen, it expires quickly |
| **Rotate client secrets regularly** | Old secrets become useless |
| **IP Whitelisting on Keycloak** | Only allow token requests from known IPs |
| **Monitor for anomalous token usage** | Alert on unusual request patterns |
| **Never store secrets in code / git** | Use environment variables or secret managers (HashiCorp Vault, AWS Secrets Manager) |
| **Use short-lived tokens + refresh tokens** | Users re-authenticate periodically |

> **ELI5:** The `client_id` + `client_secret` is like a **master key to a key dispenser**. Steal it and you can keep making new keys forever. The fix is to change the lock (rotate the secret) and only let trusted machines near the dispenser (IP whitelist).

---

## Q2 — `oauth2-resource-server` vs `oauth2-client` — what's the difference?

These serve **completely opposite roles** in the OAuth2 world:

| | `spring-boot-starter-oauth2-resource-server` | `spring-security-oauth2-client` |
|--|----------------------------------------------|----------------------------------|
| **Your service's role** | Protected resource — **receives** tokens | Client — **obtains & sends** tokens |
| **Direction** | Someone calls YOUR service with a JWT | YOUR service calls another protected service |
| **What it does** | Validates incoming JWTs from callers | Handles login flows, fetches tokens from Auth Server |
| **Typical use** | Microservice APIs, backend services | Frontend apps, API Gateways, BFF pattern |
| **In this project** | ✅ Used by `product-service` | ❌ Not used here |

### Visual:

```
[User / Client App]      [product-service]          [order-service]
        │                       │                          │
        │─── JWT in header ────►│                          │
        │                       │  (validates JWT)         │
        │                       │                          │
        │                       │─── needs to call ───────►│
        │                       │    attaches its own       │
        │                       │    token automatically    │
        │                       │                          │
        │               oauth2-resource-server       oauth2-client
        │               (validates tokens it          (gets & sends tokens
        │                receives)                     to other services)
```

> **ELI5:**
> - `resource-server` = You are the **nightclub room**. You check wristbands at your door.
> - `oauth2-client` = You are the **guest who needs to get a wristband** and show it somewhere else.

---

## Q3 — Is the token a Bearer Token? Can Keycloak give Access + Refresh Tokens?

### Yes — it IS called a Bearer Token

The word **"Bearer"** comes from how it is sent in the HTTP header:

```
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
                ↑
         This word literally means:
         "whoever BEARS (carries) this token gets access"
```

> **ELI5:** A Bearer Token is like a **bus ticket** — whoever holds it can ride, no questions asked. You do not need to prove your identity again; just showing the ticket is enough. That is exactly why it must be kept secret.

---

### Yes — Keycloak returns THREE token types:

```json
POST /token  →  {
  "access_token":  "eyJ...",    ← SHORT-LIVED  (5–15 min)
  "refresh_token": "eyJ...",    ← LONG-LIVED   (hours / days)
  "id_token":      "eyJ..."     ← IDENTITY INFO (only with openid scope)
}
```

| Token | Purpose | Lifespan | Sent to API? |
|-------|---------|----------|-------------|
| **Access Token** | Proves authorization to call APIs | Short (5–15 min) | ✅ Yes — `Authorization: Bearer` header |
| **Refresh Token** | Gets a new Access Token when it expires | Long (hours/days) | ❌ No — only sent back to Auth Server |
| **ID Token** | Contains user identity (name, email) — OIDC only | Short | ❌ No — consumed by the client app |

### The Refresh Flow:

```
1.  Login  →  Access Token (5 min) + Refresh Token (8 hours)
2.  Use Access Token to call /api/products  ✅
3.  After 5 min — Access Token expires
4.  Client sends Refresh Token to Keycloak
5.  Keycloak issues a brand new Access Token
6.  Repeat step 2  ← user never logs in again for 8 hours
```

> **ELI5:** The Access Token is a **day pass** to the museum. The Refresh Token is a **season pass** that lets you print a new day pass each morning without going back to prove who you are.

---

## Q4 — JWT vs OAuth2 — what's the difference? Why do we need both?

### The Core Distinction:

| | OAuth2 | JWT |
|--|--------|-----|
| **What it is** | A **protocol / framework** | A **token format** |
| **Defines** | WHO can access WHAT, and HOW tokens are issued | WHAT a token looks like and how to verify it |
| **Answers the question** | "How does authorization flow work?" | "What does the credential look like inside?" |
| **Mandatory link** | OAuth2 can use opaque tokens (random strings) | JWT can exist without OAuth2 entirely |

### ELI5 Analogy — The Passport Office:

> - **OAuth2** = The **rules and process** of the passport office:
>   - What forms to fill out
>   - Who qualifies to apply
>   - How long passports are valid
>   - Where and how to show the passport
>
> - **JWT** = The **physical passport format** itself:
>   - Laminated booklet with a photo, name, expiry date
>   - A government stamp that cannot be faked
>
> You can have passport rules with a different format (like a chip card = opaque token).
> JWT is just the standard booklet format that OAuth2 has adopted.

### Without JWT — OAuth2 uses Opaque Tokens:

```
Client ──── token: "abc123xyz" ────►  Resource Server
                                            │
                                 "Is abc123xyz valid?"
                                            ▼
                                       Auth Server
                                            │
                                 "Yes, belongs to user X"
                                       Resource Server ← validated ✅
```

❌ **Problem:** Every API call requires a round-trip to the Auth Server → **slow**.

### With JWT — no Auth Server round-trip:

```
Client ──── JWT token ────►  Resource Server
                                   │
                     Validates signature locally
                     using cached public key
                     (NO call to Auth Server)
                                   │
                              validated ✅  ⚡ fast
```

✅ **Advantage:** JWT is self-contained — the resource server verifies it locally without calling Keycloak on every request.

### The Relationship — One Sentence:

> **OAuth2 is the security SYSTEM. JWT is the token FORMAT that system uses.**

```
            OAuth2 Framework
    ┌───────────────────────────────────┐
    │                                   │
    │  Token issuance   ──►  JWT format │
    │  Token transport  ──►  Bearer     │
    │  Token validation ──►  Signature  │
    │                                   │
    └───────────────────────────────────┘
```

### Why not just use JWT alone (without OAuth2)?

You could — but then you would have to build everything yourself:
- Your own login flow
- Your own token expiry and refresh rules
- Your own client registration system
- Your own security edge-case handling

OAuth2 is a **battle-tested, standardized protocol** that handles all of this. JWT is just riding inside it as the token format of choice.

### Summary Cheat Sheet:

| Question | Answer |
|----------|--------|
| Is JWT part of OAuth2? | No — JWT is a separate RFC (7519). OAuth2 *chooses* to use JWT. |
| Can OAuth2 work without JWT? | Yes — it can use opaque tokens (random strings) |
| Can JWT exist without OAuth2? | Yes — JWT is just a signed JSON format usable in any system |
| Why use JWT inside OAuth2? | Self-contained verification, no Auth Server round-trip per request |
| What is `Bearer` in the header? | RFC word meaning "whoever presents this token gets access" |
| What tokens does Keycloak give? | Access Token + Refresh Token + ID Token |

---

> [← Back to README](../README.md) | [Security Deep-Dive →](security.md) | [API Reference →](api-reference.md)
