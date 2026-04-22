# 📋 Files Explained

> [← Back to README](../README.md)

Every source file in this project, broken down so anyone can understand what it does and why it exists.

---

## Table of Contents

1. [ProductServiceApplication.java](#productserviceapplicationjava)
2. [config/SecurityConfig.java](#configsecurityconfigjava)
3. [controller/ProductController.java](#controllerproductcontrollerjava)
4. [dto/ProductResponse.java](#dtoproductresponsejava)
5. [exception/GlobalExceptionHandler.java](#exceptionglobalexceptionhandlerjava)
6. [Agent.java](#agentjava)
7. [ClaimCalcuation.java](#claimcalcuationjava)

---

## `ProductServiceApplication.java`

**Location:** `src/main/java/com/vmc/product/ProductServiceApplication.java`  
**Role:** Application entry point — the very first thing Java runs.

```java
@SpringBootApplication   // ① Magic annotation that enables:
                         //   - @Configuration   → register beans
                         //   - @ComponentScan   → find all classes in this package
                         //   - @EnableAutoConfiguration → auto-wire Spring Security,
                         //                               Actuator, MVC, etc.
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);  // ② Boot the app
    }
}
```

> ELI5: This is the **power button**. When you press it, Spring wakes up, reads your config,
> finds all your controllers and beans, and starts the embedded Tomcat server.

---

## `config/SecurityConfig.java`

**Location:** `src/main/java/com/vmc/product/config/SecurityConfig.java`  
**Role:** Defines ALL security rules — who can access what, and how tokens are validated.

> See the [Security Guide](security.md) for a deep-dive ELI5 explanation of every line.

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())           // Safe for stateless REST APIs
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()  // K8s health checks = public
                .anyRequest().authenticated()                 // All else = needs JWT
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}));  // Enable JWT validation

        return http.build();
    }
}
```

---

## `controller/ProductController.java`

**Location:** `src/main/java/com/vmc/product/controller/ProductController.java`  
**Role:** Handles incoming HTTP requests and returns product data.

```java
@RestController                      // Marks this as a REST controller (returns JSON)
@RequestMapping("/api/products")     // All methods in this class use this URL prefix
public class ProductController {

    @GetMapping                      // Handles: GET /api/products
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        // Currently returns hardcoded mock data
        // TODO: Replace with a real database call via ProductService + Repository
        List<ProductResponse> products = List.of(
            new ProductResponse("P100", "Microservices Architecture", "Advanced Guide", new BigDecimal("49.99")),
            new ProductResponse("P200", "Spring Boot 4 In Action", "Latest features", new BigDecimal("59.99"))
        );
        return ResponseEntity.ok(products);  // Wraps the list in HTTP 200 OK
    }

    @GetMapping("/{id}")             // Handles: GET /api/products/{id}
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        // @PathVariable extracts {id} from the URL → e.g. /api/products/P100 → id = "P100"
        return ResponseEntity.ok(new ProductResponse(id, "Mock Product", "Description", new BigDecimal("100.00")));
    }
}
```

### Endpoint Summary

| Method | URL | Description | Auth |
|--------|-----|-------------|------|
| `GET` | `/api/products` | List all products | ✅ JWT required |
| `GET` | `/api/products/{id}` | Get product by ID | ✅ JWT required |

> ⚠️ Both endpoints currently return **hardcoded/mock data** — no database is connected yet.

---

## `dto/ProductResponse.java`

**Location:** `src/main/java/com/vmc/product/dto/ProductResponse.java`  
**Role:** Defines the shape of data returned to the caller (Data Transfer Object).

```java
/**
 * Using Java Records for concise, immutable DTOs
 */
public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price      // BigDecimal = exact decimal precision for money
) {}
```

### Why a Java Record?

> ELI5: A record is like a Google Form. You define the fields once, and Java automatically writes all the boring code for you.

A Java `record` auto-generates:

| Auto-generated | What it is |
|----------------|-----------|
| `public ProductResponse(String id, ...)` | Constructor |
| `id()`, `name()`, `description()`, `price()` | Getters |
| `equals()` | Compares two records by field values |
| `hashCode()` | Needed for maps/sets |
| `toString()` | `ProductResponse[id=P100, name=...]` |

### Why `BigDecimal` for price?

```java
// ❌ Wrong — floating-point imprecision
double price = 49.99 + 59.99;   // → 109.97999999999999

// ✅ Correct — exact decimal arithmetic
BigDecimal price = new BigDecimal("49.99").add(new BigDecimal("59.99")); // → 109.98
```

**Never use `double` or `float` for money.**

### What does the JSON output look like?

```json
{
  "id": "P100",
  "name": "Microservices Architecture",
  "description": "Advanced Guide",
  "price": 49.99
}
```

---

## `exception/GlobalExceptionHandler.java`

**Location:** `src/main/java/com/vmc/product/exception/GlobalExceptionHandler.java`  
**Role:** A single place that catches all unhandled exceptions from any controller and returns a clean JSON error response.

```java
@RestControllerAdvice           // ① Monitors ALL @RestController classes for exceptions
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)  // ② Catch any unhandled exception
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)   // ③ Return HTTP 500
                .body(Map.of(
                    "error",   "An unexpected error occurred",
                    "message", ex.getMessage()              // ④ Include the exception message
                ));
    }
}
```

### Without this handler vs with it:

| Scenario | Without Handler | With Handler |
|----------|----------------|--------------|
| Unexpected exception | Spring's default ugly HTML error page | Clean JSON `{"error": "...", "message": "..."}` |
| HTTP Status | Varies (`500` or wrong code) | Always correct `500` |
| Client experience | Confusing stack trace | Consistent, parseable JSON |

> ELI5: This is the **safety net**. If any controller throws an unexpected error, instead of crashing and showing an ugly error page, this class catches it and says "something went wrong" in a polite, structured JSON format.

---

## `Agent.java`

**Location:** `src/main/java/com/vmc/product/Agent.java`  
**Role:** Practice/scratch POJO — not part of the HTTP API.

```java
public class Agent {
    Double cliamAount;   // ⚠️ Typo: should be "claimAmount"
    Integer months;

    // Manual constructor, getters, setters, toString()
}
```

> This is a **plain old Java object** created for Streams practice (used by `ClaimCalcuation.java`).
> It has **no impact** on the running web service.
>
> 💡 In production code, this would use `@Data` from Lombok to auto-generate the boilerplate.

---

## `ClaimCalcuation.java`

**Location:** `src/main/java/com/vmc/product/ClaimCalcuation.java`  
**Role:** Standalone Java Streams practice — not part of the HTTP API.

```java
public class ClaimCalcuation {

    public static void main(String[] args) {
        // Create 5 agents with claim amounts and months
        Agent agent1 = new Agent(60000.0, 7);
        Agent agent2 = new Agent(40000.0, 5);
        Agent agent3 = new Agent(70000.0, 5);
        Agent agent4 = new Agent(40000.0, 5);
        Agent agent5 = new Agent(60000.0, 5);

        List<Agent> agentList = Arrays.asList(agent1, agent2, agent3, agent4, agent5);

        Double avgAmount = agentList.stream()
            .filter(agent -> agent.getMonths() <= 6)          // Keep: months ≤ 6
            .filter(agent -> agent.getCliamAount() > 50000)   // Keep: claim > 50,000
            .collect(Collectors.averagingDouble(agent -> agent.getCliamAount())); // Average

        System.out.println(avgAmount);  // Output: 65000.0
    }
}
```

### Stream Pipeline Walkthrough

| Step | Agents remaining | Reason |
|------|-----------------|--------|
| Start | agent1, 2, 3, 4, 5 | All 5 agents |
| `filter(months <= 6)` | agent2, 3, 4, 5 | agent1 has 7 months → excluded |
| `filter(claimAmount > 50000)` | agent3, agent5 | agent2 (40k) and agent4 (40k) excluded |
| `averagingDouble(...)` | `65000.0` | (70000 + 60000) / 2 |

> ELI5: It's like filtering students who both scored > 80% **AND** attended > 75% classes, then averaging their scores. Two filters, then calculate the average.

> ⚠️ **Note:** `ClaimCalcuation` has two typos: `ClaimCalcuation` (missing `l`) and `cliamAount` (transposed letters). See the [Roadmap](roadmap.md) for cleanup tasks.

---

> [← Back to README](../README.md) | [Security →](security.md) | [API Reference →](api-reference.md)
