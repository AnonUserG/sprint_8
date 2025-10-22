package ru.practicum.onlineStore.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.model.Item;

import java.math.BigDecimal;
import java.util.List;
import reactor.test.StepVerifier;



@DataR2dbcTest
@ActiveProfiles("test")
public class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void cleanDb() {
        itemRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Сохранение и поиск по id")
    void saveAndFindById() {
        Item item = new Item();
        item.setTitle("Чашка Spring");
        item.setDescription("Керамическая кружка с логотипом Spring");
        item.setPrice(BigDecimal.valueOf(500));

        Mono<Item> savedMono = itemRepository.save(item);

        StepVerifier.create(savedMono.flatMap(saved ->
                        itemRepository.findById(saved.getId())))
                .expectNextMatches(found ->
                        found.getTitle().equals("Чашка Spring") &&
                                found.getPrice().compareTo(BigDecimal.valueOf(500)) == 0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Поиск всех товаров возвращает список")
    void findAll_ReturnsItems() {
        Item item1 = new Item();
        item1.setTitle("Тетрадь");
        item1.setPrice(BigDecimal.valueOf(100));

        Item item2 = new Item();
        item2.setTitle("Ручка");
        item2.setPrice(BigDecimal.valueOf(50));

        Mono<Void> saveAllMono = itemRepository.saveAll(List.of(item1, item2)).then();

        StepVerifier.create(saveAllMono.thenMany(itemRepository.findAll()).collectList())
                .expectNextMatches(items ->
                        items.size() == 2 &&
                                items.stream().map(Item::getTitle).toList().containsAll(List.of("Тетрадь", "Ручка"))
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Удаление товара по id")
    void deleteById_RemovesItem() {
        Item item = new Item();
        item.setTitle("Стикеры");
        item.setPrice(BigDecimal.valueOf(30));

        Mono<Item> savedMono = itemRepository.save(item);

        StepVerifier.create(savedMono.flatMap(saved ->
                        itemRepository.deleteById(saved.getId())
                                .then(itemRepository.findById(saved.getId()))))
                .expectNextCount(0) // Ожидаем, что элемент не найден
                .verifyComplete();
    }
}
