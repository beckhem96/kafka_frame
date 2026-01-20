package com.example.kafka.dto;

import lombok.Data;

/**
* 주문 DTO
 */
@Data
public class OrderRequest {
    private String productId;
    private Double amount;
    private Integer quantity;

}
