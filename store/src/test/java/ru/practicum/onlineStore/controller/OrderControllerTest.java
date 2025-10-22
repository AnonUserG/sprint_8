package ru.practicum.onlineStore.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.client.api.DefaultApi;
import org.openapitools.client.model.PaymentRequest;
import org.openapitools.client.model.PaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.model.Item;
import ru.practicum.onlineStore.model.Order;
import ru.practicum.onlineStore.model.OrderItem;
import ru.practicum.onlineStore.repository.ItemRepository;
import ru.practicum.onlineStore.repository.OrderItemRepository;
import ru.practicum.onlineStore.service.CartService;
import ru.practicum.onlineStore.service.OrderService;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@WebFluxTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private ItemRepository itemRepository;

    @MockitoBean
    private DefaultApi defaultApi;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                    .build();
        }
    }

    @Test
    @DisplayName("POST /orders/buy")
    void buy_ShouldCreateOrderAndRedirect() {
        Item item1 = Item.builder().id(1L).price(BigDecimal.valueOf(100)).build();
        Order order1 = Order.builder().id(1L).build();

        when(cartService.getCart()).thenReturn(Mono.just(Map.of(item1, 2)));
        when(defaultApi.apiPaymentsPayPost(any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse().success(true)));
        when(orderService.createOrder(any())).thenReturn(Mono.just(order1));
        when(cartService.clear()).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/orders/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/1?newOrder=true");

        verify(defaultApi).apiPaymentsPayPost(any(PaymentRequest.class));
        verify(orderService).createOrder(any());
        verify(cartService).clear();
    }

    @Test
    @DisplayName("GET /orders")
    void listOrders_ShouldReturnOrdersView() {
        Order order1 = Order.builder().id(1L).build();
        Item item1 = Item.builder().id(1L).price(BigDecimal.valueOf(100)).build();
        OrderItem orderItem1 = OrderItem.builder().itemId(1L).count(2).price(BigDecimal.valueOf(100)).build();

        when(orderService.findAll()).thenReturn(Flux.just(order1));
        when(orderItemRepository.findByOrderId(order1.getId())).thenReturn(Flux.just(orderItem1));
        when(itemRepository.findById(orderItem1.getItemId())).thenReturn(Mono.just(item1));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk();

        verify(orderService).findAll();
        verify(orderItemRepository).findByOrderId(order1.getId());
        verify(itemRepository).findById(orderItem1.getItemId());
    }

    @Test
    @DisplayName("GET /orders/{id}")
    void showOrder_ShouldReturnOrderView() {
        Order order1 = new Order();
        order1.setId(1L);

        Item item1 = Item.builder().id(1L).price(BigDecimal.valueOf(100)).build();
        OrderItem orderItem1 = OrderItem.builder()
                .itemId(1L)
                .count(2)
                .price(BigDecimal.valueOf(100))
                .build();

        when(orderService.findById(order1.getId())).thenReturn(Mono.just(order1));
        when(orderItemRepository.findByOrderId(order1.getId())).thenReturn(Flux.just(orderItem1));
        when(itemRepository.findById(orderItem1.getItemId())).thenReturn(Mono.just(item1));

        webTestClient.get()
                .uri("/orders/{id}", 1L)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                });

        verify(orderService).findById(order1.getId());
        verify(orderItemRepository).findByOrderId(order1.getId());
        verify(itemRepository, atLeastOnce()).findById(orderItem1.getItemId());
    }
}
