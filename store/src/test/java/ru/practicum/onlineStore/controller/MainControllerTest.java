package ru.practicum.onlineStore.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.model.Item;
import ru.practicum.onlineStore.service.CartService;
import ru.practicum.onlineStore.service.ItemService;

import java.math.BigDecimal;
import java.util.Map;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@WebFluxTest(MainController.class)
public class MainControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    private Item item1;
    private Item item2;

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

    @BeforeEach
    void setUp() {
        item1 = Item.builder()
                .id(1L)
                .title("Кружка")
                .description("Белая кружка")
                .price(BigDecimal.valueOf(500))
                .imgPath("mug.jpg")
                .count(0)
                .build();

        item2 = Item.builder()
                .id(2L)
                .title("Футболка")
                .description("Черная футболка")
                .price(BigDecimal.valueOf(1200))
                .imgPath("tshirt.jpg")
                .count(0)
                .build();

        when(cartService.getCartItemsCount()).thenReturn(Mono.just(Map.of(1L, 2)));
        when(itemService.findAll()).thenReturn(Flux.just(item1, item2));
        when(itemService.findById(1L)).thenReturn(Mono.just(item1));
        when(itemService.findById(2L)).thenReturn(Mono.just(item2));

        when(cartService.addItem(any(Item.class))).thenReturn(Mono.empty());
        when(cartService.removeOne(any(Item.class))).thenReturn(Mono.empty());
        when(cartService.deleteItem(any(Item.class))).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("GET / редирект на /main/items")
    void rootRedirect_ShouldRedirect() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main/items");
    }

    @Test
    @DisplayName("GET /main/items отображает список товаров с пагинацией и корзиной")
    void showItems_ShouldReturnItemsPage() {
        webTestClient.get()
                .uri("/main/items?search=&sort=NO&pageSize=10&pageNumber=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(res ->
                        org.assertj.core.api.Assertions.assertThat(res.getResponseBody())
                                .contains("Кружка")
                                .contains("Футболка")
                                .contains("main")
                );

        verify(itemService).findAll();
        verify(cartService).getCartItemsCount();
    }

    @Test
    @DisplayName("POST /main/items/{id} с action=PLUS → редиректит и вызывает addItem()")
    void updateCartFromMain_PlusAction_ShouldAddItem() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("action", "plus");

        webTestClient.post()
                .uri("/main/items/{id}", 1L)
                .bodyValue(formData)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main/items");

        verify(cartService).addItem(item1);
    }

    @Test
    @DisplayName("POST /main/items/{id} без action → редиректит")
    void updateCartFromMain_NoAction_ShouldRedirect() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        webTestClient.post()
                .uri("/main/items/{id}", 1L)
                .bodyValue(formData)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main/items");
    }

    @Test
    @DisplayName("GET /items/{id} отображает страницу товара с корзиной")
    void showItem_ShouldReturnItemPage() {
        webTestClient.get()
                .uri("/items/{id}", 1L)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(res ->
                        org.assertj.core.api.Assertions.assertThat(res.getResponseBody())
                                .contains("Кружка")
                                .contains("item")
                );

        verify(itemService).findById(1L);
        verify(cartService).getCartItemsCount();
    }

    @Test
    @DisplayName("POST /items/{id} с action=DELETE")
    void updateCartFromItem_DeleteAction_ShouldDeleteItem() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("action", "delete");

        webTestClient.post()
                .uri("/items/{id}", 1L)
                .bodyValue(formData)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/1");

        verify(cartService).deleteItem(item1);
    }
}
