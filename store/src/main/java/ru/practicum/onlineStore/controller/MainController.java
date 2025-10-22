package ru.practicum.onlineStore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.model.Item;
import ru.practicum.onlineStore.service.CartService;
import ru.practicum.onlineStore.service.ItemService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping("/")
    public Mono<String> rootRedirect() {
        return Mono.just("redirect:/main/items");
    }

    @GetMapping("/main/items")
    public Mono<Rendering> showItems(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") String sort,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNumber
    ) {
        Mono<List<Item>> filteredItemsMono = itemService.findAll()
                .filter(i -> i.getTitle().toLowerCase().contains(search.toLowerCase()) ||
                        i.getDescription().toLowerCase().contains(search.toLowerCase()))
                .collectList()
                .map(filtered -> {
                    switch (sort) {
                        case "ALPHA" -> filtered.sort(Comparator.comparing(Item::getTitle));
                        case "PRICE" -> filtered.sort(Comparator.comparing(Item::getPrice));
                    }
                    return filtered;
                });

        Mono<Map<Long, Integer>> cartCountsMono = cartService.getCartItemsCount();

        return Mono.zip(filteredItemsMono, cartCountsMono)
                .map(tuple -> {
                    List<Item> filtered = tuple.getT1();
                    Map<Long, Integer> cartCounts = tuple.getT2();

                    int fromIndex = Math.min((pageNumber - 1) * pageSize, filtered.size());
                    int toIndex = Math.min(fromIndex + pageSize, filtered.size());
                    List<Item> pageItems = filtered.subList(fromIndex, toIndex);

                    List<List<Item>> itemsRows = pageItems.stream()
                            .collect(Collectors.groupingBy(i -> pageItems.indexOf(i) / 3))
                            .values().stream().toList();

                    return Rendering.view("main")
                            .modelAttribute("items", itemsRows)
                            .modelAttribute("cartCounts", cartCounts)
                            .modelAttribute("search", search)
                            .modelAttribute("sort", sort)
                            .modelAttribute("paging", new Object() {
                                public int pageNumber() { return pageNumber; }
                                public int pageSize() { return pageSize; }
                                public boolean hasNext() { return toIndex < filtered.size(); }
                                public boolean hasPrevious() { return pageNumber > 1; }
                            })
                            .build();
                });
    }

    @PostMapping("/main/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public Mono<Rendering> updateCartFromMain(@PathVariable Long id, ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String action = formData.getFirst("action");

                    if (action == null) {
                        return Mono.just(Rendering.redirectTo("/main/items").build());
                    }

                    return itemService.findById(id)
                            .flatMap(item -> switch (action.toLowerCase()) {
                                case "plus" -> cartService.addItem(item);
                                case "minus" -> cartService.removeOne(item);
                                case "addtocart" -> cartService.addItem(item);
                                default -> Mono.empty();
                            })
                            .then(Mono.just(Rendering.redirectTo("/main/items").build()));
                });
    }


    @GetMapping("/items/{id}")
    public Mono<Rendering> showItem(@PathVariable Long id) {
        Mono<Item> itemMono = itemService.findById(id);
        Mono<Map<Long, Integer>> cartCountsMono = cartService.getCartItemsCount();

        return Mono.zip(itemMono, cartCountsMono)
                .map(tuple -> Rendering.view("item")
                        .modelAttribute("item", tuple.getT1())
                        .modelAttribute("itemsCount", tuple.getT2())
                        .build());
    }

    @PostMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public Mono<String> updateCartFromItem(@PathVariable Long id, ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String action = formData.getFirst("action");
                    if (action == null) return Mono.just("redirect:/items/" + id);

                    return itemService.findById(id)
                            .flatMap(item -> switch (action.toLowerCase()) {
                                case "plus" -> cartService.addItem(item);
                                case "minus" -> cartService.removeOne(item);
                                case "delete" -> cartService.deleteItem(item);
                                default -> Mono.empty();
                            })
                            .then(Mono.just("redirect:/items/" + id));
                });
    }

}

