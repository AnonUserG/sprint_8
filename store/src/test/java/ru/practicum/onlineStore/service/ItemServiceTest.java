package ru.practicum.onlineStore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.practicum.onlineStore.model.Item;
import ru.practicum.onlineStore.repository.ItemRepository;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;


@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @InjectMocks
    private ItemService itemService;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, Object> valueOps;

    @Mock
    private ObjectMapper objectMapper;

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

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void findAll_ReturnsAllItems_FromRepo_WhenCacheEmpty() throws Exception {
        when(valueOps.get("items:all")).thenReturn(Mono.empty());
        when(itemRepository.findAll()).thenReturn(Flux.just(item1, item2));
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
        when(valueOps.set(eq("items:all"), any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(itemService.findAll())
                .expectNext(item1)
                .expectNext(item2)
                .verifyComplete();

        verify(itemRepository).findAll();
        verify(valueOps).set(eq("items:all"), any(), any());
    }

    @Test
    void findById_ReturnsItem_FromRepo_WhenCacheEmpty() throws Exception {
        when(valueOps.get("items:1")).thenReturn(Mono.empty());
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(valueOps.set(eq("items:1"), any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(itemService.findById(1L))
                .expectNext(item1)
                .verifyComplete();

        verify(itemRepository).findById(1L);
        verify(valueOps).set(eq("items:1"), any(), any());
    }

    @Test
    void save_SavesItemAndUpdatesCache() {
        when(itemRepository.save(item1)).thenReturn(Mono.just(item1));
        when(valueOps.set(eq("items:1"), eq(item1), any())).thenReturn(Mono.just(true));
        when(redisTemplate.delete("items:all")).thenReturn(Mono.just(1L));

        StepVerifier.create(itemService.save(item1))
                .expectNext(item1)
                .verifyComplete();

        verify(itemRepository).save(item1);
        verify(valueOps).set(eq("items:1"), eq(item1), any());
        verify(redisTemplate).delete("items:all");
    }

    @Test
    void delete_DeletesItemAndClearsCache() {
        when(itemRepository.deleteById(1L)).thenReturn(Mono.empty());
        when(redisTemplate.delete("items:1")).thenReturn(Mono.just(1L));
        when(redisTemplate.delete("items:all")).thenReturn(Mono.just(1L));

        StepVerifier.create(itemService.delete(1L))
                .verifyComplete();

        verify(itemRepository).deleteById(1L);
        verify(redisTemplate).delete("items:1");
        verify(redisTemplate).delete("items:all");
    }
}
