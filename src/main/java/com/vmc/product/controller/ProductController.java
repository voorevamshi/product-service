
package com.vmc.product.controller;

import com.vmc.product.dto.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController

@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = List.of(
                new ProductResponse("P100", "Microservices Architecture", "Advanced Guide", new BigDecimal("49.99")),
                new ProductResponse("P200", "Spring Boot 4 In Action", "Latest features", new BigDecimal("59.99"))
        );
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        // Mock data for initial setup
        return ResponseEntity.ok(new ProductResponse(id, "Mock Product", "Description", new BigDecimal("100.00")));
    }
}