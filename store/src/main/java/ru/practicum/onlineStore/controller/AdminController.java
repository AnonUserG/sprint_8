package ru.practicum.onlineStore.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.model.Item;
import ru.practicum.onlineStore.service.ItemService;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ItemService itemService;

    @GetMapping("/addItem")
    public Mono<Rendering> showAddItemForm() {
        return Mono.just(
                Rendering.view("add-item")
                        .modelAttribute("item", new Item())
                        .build());
    }

    @PostMapping("/addItem")
    public Mono<String> handleAddItem(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String title = formData.getFirst("title");
                    String description = formData.getFirst("description");
                    String imgPath = formData.getFirst("imgPath");
                    String countStr = formData.getFirst("count");
                    String priceStr = formData.getFirst("price");

                    if (title == null || priceStr == null) {
                        return Mono.just("redirect:/admin/addItem");
                    }

                    int count = 0;
                    if (countStr != null && !countStr.isBlank()) {
                        try {
                            count = Integer.parseInt(countStr);
                        } catch (NumberFormatException ignored) {}
                    }

                    BigDecimal price = BigDecimal.ZERO;
                    if (priceStr != null && !priceStr.isBlank()) {
                        try {
                            price = new BigDecimal(priceStr);
                        } catch (NumberFormatException ignored) {}
                    }

                    String normalizedPath = imgPath;
                    if (imgPath.contains("static")) {
                        int idx = imgPath.lastIndexOf("static") + "static".length() + 1;
                        normalizedPath = imgPath.substring(idx);
                    }

                    Item item = new Item();
                    item.setTitle(title);
                    item.setDescription(description != null ? description : "");
                    item.setImgPath(normalizedPath);
                    item.setCount(count);
                    item.setPrice(price);

                    return itemService.save(item)
                            .then(Mono.just("redirect:/main/items"));
                });
    }


}
