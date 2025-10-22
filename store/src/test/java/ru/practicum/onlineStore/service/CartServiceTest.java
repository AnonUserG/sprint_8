package ru.practicum.onlineStore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.onlineStore.model.Item;

import java.math.BigDecimal;



import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;


public class CartServiceTest {

    private CartService cartService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        cartService = new CartService();

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
    @DisplayName("Добавление товаров в корзину")
    void addItem_IncreasesCount() {
        StepVerifier.create(cartService.addItem(item1).then(cartService.addItem(item1)))
                .verifyComplete();

        StepVerifier.create(cartService.getCartItemsCount())
                .assertNext(map -> assertThat(map.get(item1.getId())).isEqualTo(2))
                .verifyComplete();
    }

    @Test
    @DisplayName("Удаление одного экземпляра товара")
    void removeOne_DecreasesCountOrRemoves() {
        StepVerifier.create(cartService.addItem(item1).then(cartService.addItem(item1)))
                .verifyComplete();

        StepVerifier.create(cartService.removeOne(item1))
                .verifyComplete();

        StepVerifier.create(cartService.getCartItemsCount())
                .assertNext(map -> assertThat(map.get(item1.getId())).isEqualTo(1))
                .verifyComplete();

        StepVerifier.create(cartService.removeOne(item1))
                .verifyComplete();

        StepVerifier.create(cartService.getCartItemsCount())
                .assertNext(map -> assertThat(map).doesNotContainKey(item1.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Полное удаление товара")
    void deleteItem_RemovesItem() {
        StepVerifier.create(cartService.addItem(item1))
                .verifyComplete();

        StepVerifier.create(cartService.deleteItem(item1))
                .verifyComplete();

        StepVerifier.create(cartService.getCartItemsCount())
                .assertNext(map -> assertThat(map).doesNotContainKey(item1.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Очистка корзины")
    void clear_RemovesAllItems() {
        StepVerifier.create(cartService.addItem(item1).then(cartService.addItem(item2)))
                .verifyComplete();

        StepVerifier.create(cartService.clear())
                .verifyComplete();

        StepVerifier.create(cartService.isEmpty())
                .assertNext(empty -> assertThat(empty).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("Подсчет общей суммы корзины")
    void getTotal_CalculatesCorrectly() {
        StepVerifier.create(cartService.addItem(item1).then(cartService.addItem(item2)).then(cartService.addItem(item2)))
                .verifyComplete();

        StepVerifier.create(cartService.getTotal())
                .assertNext(total -> assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(2900)))
                .verifyComplete();
    }

    @Test
    @DisplayName("Проверка пустоты корзины")
    void isEmpty_ReturnsCorrectValue() {
        StepVerifier.create(cartService.isEmpty())
                .assertNext(empty -> assertThat(empty).isTrue())
                .verifyComplete();

        StepVerifier.create(cartService.addItem(item1))
                .verifyComplete();

        StepVerifier.create(cartService.isEmpty())
                .assertNext(empty -> assertThat(empty).isFalse())
                .verifyComplete();
    }
}