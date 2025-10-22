package ru.practicum.onlineStore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.practicum.onlineStore.model.Item;
import ru.practicum.onlineStore.model.Order;
import ru.practicum.onlineStore.model.OrderItem;
import ru.practicum.onlineStore.repository.OrderItemRepository;
import ru.practicum.onlineStore.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        item1 = new Item();
        item1.setId(1L);
        item1.setTitle("Кружка");
        item1.setPrice(BigDecimal.valueOf(500));

        item2 = new Item();
        item2.setId(2L);
        item2.setTitle("Футболка");
        item2.setPrice(BigDecimal.valueOf(1200));
    }

    @Test
    @DisplayName("Сохраняем заказ")
    void saveOrder_Success() {
        Order order = new Order();
        order.setId(1L);

        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.save(new Order()))
                .expectNextMatches(saved -> saved.getId() == 1L)
                .verifyComplete();

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Поиск всех заказов")
    void findAll_ReturnsOrders() {
        Order order1 = new Order();
        order1.setId(1L);
        Order order2 = new Order();
        order2.setId(2L);

        when(orderRepository.findAll()).thenReturn(Flux.just(order1, order2));

        StepVerifier.create(orderService.findAll())
                .expectNext(order1)
                .expectNext(order2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Поиск заказа по ID")
    void findById_ReturnsOrder() {
        Order order = new Order();
        order.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.findById(1L))
                .expectNextMatches(found -> found.getId() == 1L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Создание заказа с товарами")
    void createOrder_WithItems() {
        OrderItem oi1 = new OrderItem();
        oi1.setItem(item1);
        oi1.setCount(1);
        oi1.setPrice(item1.getPrice());

        OrderItem oi2 = new OrderItem();
        oi2.setItem(item2);
        oi2.setCount(2);
        oi2.setPrice(item2.getPrice());

        List<OrderItem> orderItems = List.of(oi1, oi2);
        Order savedOrder = new Order();
        savedOrder.setId(1L);

        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(orderService.createOrder(Flux.fromIterable(orderItems)))
                .expectNextMatches(order -> order.getId() == 1L)
                .verifyComplete();

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
    }
}
