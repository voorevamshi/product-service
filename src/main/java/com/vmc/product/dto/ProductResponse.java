package com.vmc.product.dto;

import java.math.BigDecimal;

/**
 * Using Java Records for concise, immutable DTOs
 */
public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price
) {}