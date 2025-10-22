package ru.practicum.onlineStore.model;

import org.springframework.data.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    private Long id;

    private String title;

    private String description;

    private String imgPath;

    private int count;

    private BigDecimal price;

}
