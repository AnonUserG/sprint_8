package ru.practicum.onlineStore.service;

import lombok.Getter;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.model.Item;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final Map<String, Map<Item, Integer>> carts = new ConcurrentHashMap<>();

    private Mono<String> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .defaultIfEmpty("ANON");
    }

    public Mono<Void> addItem(Item item) {
        return getCurrentUser().doOnNext(user ->
                carts.computeIfAbsent(user, u -> new ConcurrentHashMap<>())
                        .merge(item, 1, Integer::sum)
        ).then();
    }

    public Mono<Void> removeOne(Item item) {
        return getCurrentUser().doOnNext(user -> {
            Map<Item, Integer> cart = carts.get(user);
            if (cart != null) {
                cart.computeIfPresent(item, (k, v) -> v > 1 ? v - 1 : null);
            }
        }).then();
    }

    public Mono<Void> deleteItem(Item item) {
        return getCurrentUser().doOnNext(user -> {
            Map<Item, Integer> cart = carts.get(user);
            if (cart != null) {
                cart.remove(item);
            }
        }).then();
    }

    public Mono<Void> clear() {
        return getCurrentUser().doOnNext(user -> carts.remove(user)).then();
    }

    public Mono<BigDecimal> getTotal() {
        return getCurrentUser().map(user ->
                carts.getOrDefault(user, Map.of())
                        .entrySet()
                        .stream()
                        .map(e -> e.getKey().getPrice().multiply(BigDecimal.valueOf(e.getValue())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    public Mono<Boolean> isEmpty() {
        return getCurrentUser().map(user -> {
            Map<Item, Integer> cart = carts.get(user);
            return cart == null || cart.isEmpty();
        });
    }

    public Mono<Map<Long, Integer>> getCartItemsCount() {
        return getCurrentUser().map(user -> {
            Map<Item, Integer> userCart = carts.getOrDefault(user, Map.of());

            Map<Item, Integer> safeCopy = new ConcurrentHashMap<>(userCart);

            return safeCopy.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().getId(),
                            Map.Entry::getValue,
                            Integer::sum // объединяем дубликаты суммированием
                    ));
        });
    }



    public Mono<Map<Item, Integer>> getCart() {
        return getCurrentUser().map(user ->
                carts.getOrDefault(user, Map.of())
        );
    }
}
