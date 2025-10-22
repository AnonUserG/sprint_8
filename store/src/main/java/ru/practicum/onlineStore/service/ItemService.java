package ru.practicum.onlineStore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.model.Item;
import ru.practicum.onlineStore.repository.ItemRepository;

import java.time.Duration;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public Flux<Item> findAll() {
        return redisTemplate.opsForValue().get("items:all")
                .cast(String.class) // получаем JSON
                .flatMapMany(json -> {
                    try {
                        List<Item> items = objectMapper.readValue(json, new TypeReference<List<Item>>() {});
                        return Flux.fromIterable(items);
                    } catch (Exception e) {
                        return Flux.empty();
                    }
                })
                .switchIfEmpty(
                        itemRepository.findAll()
                                .collectList()
                                .flatMapMany(list -> {
                                    try {
                                        String json = objectMapper.writeValueAsString(list);
                                        return redisTemplate.opsForValue()
                                                .set("items:all", json, Duration.ofMinutes(10))
                                                .thenMany(Flux.fromIterable(list));
                                    } catch (Exception e) {
                                        return Flux.fromIterable(list);
                                    }
                                })
                );
    }


    public Mono<Item> findById(Long id) {
        return redisTemplate.opsForValue().get("items:" + id) // возвращает Object
                .flatMap(obj -> {
                    try {
                        String json = objectMapper.writeValueAsString(obj);
                        Item item = objectMapper.readValue(json, Item.class);
                        return Mono.just(item);
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(
                        itemRepository.findById(id)
                                .flatMap(item -> {
                                    try {
                                        redisTemplate.opsForValue()
                                                .set("items:" + item.getId(), item, Duration.ofMinutes(10))
                                                .subscribe();
                                    } catch (Exception ignored) {}
                                    return Mono.just(item);
                                })
                );
    }



    public Mono<Item> save(Item item) {
        return itemRepository.save(item)
                .flatMap(saved -> {
                    String key = "items:" + saved.getId();
                    return redisTemplate.opsForValue().set(key, saved, Duration.ofMinutes(10))
                            .then(redisTemplate.delete("items:all"))
                            .thenReturn(saved);
                });
    }


    public Mono<Void> delete(Long id) {
        return itemRepository.deleteById(id)
                .then(redisTemplate.delete("items:" + id))
                .then(redisTemplate.delete("items:all"))
                .then();
    }

}
