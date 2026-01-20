package com.example.kafka.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.protocol.types.Field;

@Getter
@Setter
@Builder
public class Order {
    private String id;
    private Integer amount;
}
