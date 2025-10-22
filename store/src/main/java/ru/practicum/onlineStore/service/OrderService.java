package ru.practicum.onlineStore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.model.Order;
import ru.practicum.onlineStore.model.OrderItem;
import ru.practicum.onlineStore.repository.OrderItemRepository;
import ru.practicum.onlineStore.repository.OrderRepository;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Flux<Order> findAll() {
        return orderRepository.findAll();
    }

    public Mono<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Mono<Order> save(Order order) {
        return orderRepository.save(order);
    }

    public Mono<Order> createOrder(Flux<OrderItem> itemsFlux) {
        Order order = new Order();
        order.setCreatedAt(java.time.LocalDateTime.now());

        return orderRepository.save(order)
                .flatMap(savedOrder ->
                        itemsFlux
                                .map(item -> {
                                    item.setOrderId(savedOrder.getId());
                                    return item;
                                })
                                .flatMap(orderItemRepository::save)
                                .then(Mono.just(savedOrder))
                );
    }
}
