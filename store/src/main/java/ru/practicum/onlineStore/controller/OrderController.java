package ru.practicum.onlineStore.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.client.api.DefaultApi;
import org.openapitools.client.model.PaymentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.model.Order;
import ru.practicum.onlineStore.model.OrderItem;
import ru.practicum.onlineStore.repository.ItemRepository;
import ru.practicum.onlineStore.repository.OrderItemRepository;
import ru.practicum.onlineStore.service.CartService;
import ru.practicum.onlineStore.service.OrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    @Autowired
    private DefaultApi defaultApi;

    @PostMapping("/buy")
    public Mono<String> buy(Model model) {
        return cartService.getCart()
                .flatMap(cart -> {
                    List<OrderItem> orderItems = cart.entrySet().stream()
                            .map(e -> OrderItem.builder()
                                    .itemId(e.getKey().getId())
                                    .count(e.getValue())
                                    .price(e.getKey().getPrice())
                                    .build())
                            .collect(Collectors.toList());

                    BigDecimal amount = orderItems.stream()
                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    PaymentRequest paymentRequest = new PaymentRequest();
                    paymentRequest.setAccountId("defaultAccount");
                    paymentRequest.setOrderId("temp_" + System.currentTimeMillis()); // временный ID
                    paymentRequest.setAmount(amount.doubleValue());

                    return defaultApi.apiPaymentsPayPost(paymentRequest)
                            .flatMap(paymentResponse -> {
                                if (Boolean.TRUE.equals(paymentResponse.getSuccess())) {
                                    return orderService.createOrder(Flux.fromIterable(orderItems))
                                            .flatMap(savedOrder ->
                                                    cartService.clear()
                                                            .thenReturn("redirect:/orders/" + savedOrder.getId() + "?newOrder=true")
                                            );
                                } else {
                                    model.addAttribute("errorTitle", "Ошибка оплаты");
                                    model.addAttribute("errorMessage", paymentResponse.getError() != null ? paymentResponse.getError() : "Платеж не прошел");
                                    return Mono.just("error/error");
                                }
                            });
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                        model.addAttribute("errorTitle", "Ошибка оплаты");
                        model.addAttribute("errorMessage", "Платеж не прошел: сумма больше лимита");
                        return Mono.just("error/error");
                    }
                    return Mono.error(ex);
                })
                .onErrorResume(ResponseStatusException.class, ex -> {
                    log.error("Ошибка при оплате: ", ex);
                    return Mono.just("error/error");
                });
    }


    @GetMapping
    public Mono<Rendering> listOrders() {
        return orderService.findAll()
                .flatMap(order ->
                        orderItemRepository.findByOrderId(order.getId())
                                .flatMap(orderItem ->
                                        itemRepository.findById(orderItem.getItemId())
                                                .map(item -> {
                                                    orderItem.setItem(item);
                                                    orderItem.setTotal(item.getPrice().multiply(BigDecimal.valueOf(orderItem.getCount())));
                                                    return orderItem;
                                                })
                                )
                                .collectList()
                                .map(items -> {
                                    order.setItems(items);
                                    BigDecimal total = items.stream()
                                            .map(OrderItem::getTotal)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    order.setTotal(total);
                                    return order;
                                })
                )
                .collectList()
                .map(orders -> Rendering.view("orders")
                        .modelAttribute("orders", orders)
                        .build()
                );
    }




    @GetMapping("/{id}")
    public Mono<Rendering> showOrder(@PathVariable Long id,
                                     @RequestParam(defaultValue = "false") boolean newOrder) {
        Mono<Order> orderMono = orderService.findById(id);

        Flux<Map<String, Object>> itemsFlux = orderItemRepository.findByOrderId(id)
                .flatMap(orderItem -> itemRepository.findById(orderItem.getItemId())
                        .map(item -> Map.of(
                                "item", item,
                                "count", orderItem.getCount(),
                                "price", orderItem.getPrice()
                        ))
                );

        Mono<List<Map<String, Object>>> itemsMono = itemsFlux.collectList();

        Mono<BigDecimal> totalMono = itemsFlux
                .map(m -> ((BigDecimal) m.get("price")).multiply(BigDecimal.valueOf((Integer) m.get("count"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Mono.zip(orderMono, itemsMono, totalMono)
                .map(tuple -> Rendering.view("order")
                        .modelAttribute("order", tuple.getT1())
                        .modelAttribute("items", tuple.getT2())
                        .modelAttribute("total", tuple.getT3())
                        .modelAttribute("newOrder", newOrder)
                        .build());
    }
}

