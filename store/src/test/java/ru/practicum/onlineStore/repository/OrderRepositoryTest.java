package ru.practicum.onlineStore.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.practicum.onlineStore.model.Item;
import ru.practicum.onlineStore.model.Order;
import ru.practicum.onlineStore.model.OrderItem;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;


@DataR2dbcTest
@ActiveProfiles("test")
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void cleanDb() {
        orderRepository.deleteAll().block();
        itemRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Сохранение заказа с товарами")
    void saveOrder_WithItems_Success() {
        Item item = Item.builder()
                .title("Кружка")
                .price(BigDecimal.valueOf(500))
                .build();

        Mono<Order> testMono = itemRepository.save(item)
                .flatMap(savedItem -> {
                    OrderItem orderItem = OrderItem.builder()
                            .item(savedItem)
                            .count(2)
                            .price(savedItem.getPrice())
                            .build();

                    Order order = new Order();
                    order.setItems(List.of(orderItem));
                    return orderRepository.save(order);
                });

        StepVerifier.create(testMono)
                .assertNext(savedOrder -> {
                    assertThat(savedOrder.getId()).isNotNull();
                    assertThat(savedOrder.getItems()).hasSize(1);
                    assertThat(savedOrder.getItems().get(0).getItem().getTitle()).isEqualTo("Кружка");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Поиск заказа по ID")
    void findById_ReturnsOrder() {
        Order order = new Order();

        StepVerifier.create(orderRepository.save(order)
                        .flatMap(saved -> orderRepository.findById(saved.getId())))
                .assertNext(found -> assertThat(found.getId()).isNotNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("findAll возвращает список заказов")
    void findAll_ReturnsOrders() {
        Order order1 = new Order();
        Order order2 = new Order();

        StepVerifier.create(orderRepository.saveAll(Flux.just(order1, order2)).collectList()
                        .flatMapMany(savedOrders -> orderRepository.findAll().collectList()))
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    // проверяем только наличие ID, не сравниваем createdAt
                    assertThat(list).extracting(Order::getId).allMatch(id -> id != null);
                })
                .verifyComplete();
    }
}
