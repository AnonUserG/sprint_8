package ru.practicum.onlineStore.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Table("orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private Long id;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    private List<OrderItem> items;

    @Transient
    private BigDecimal total;

}
