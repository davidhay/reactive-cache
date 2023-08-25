package com.david.reactivecache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class UseCacheWithFluxTest {

    public static final String CACHE_KEY_ID = "cache-key-id";
    Map<String, Map<Integer, Integer>> cache = new ConcurrentHashMap<>();

    AtomicInteger calculatedCounter = new AtomicInteger(0);

    @BeforeEach
    void setup() {
        this.calculatedCounter.set(0);
    }

    @Test
    void test() {

        String requestId = UUID.randomUUID().toString();

        Flux<Integer> daFlux = Flux.deferContextual(ctx ->
                        Flux.just(1, 2, 1, 2, 1)
                                .map(val -> {
                                    System.out.println("VAL IS " + val);
                                    return val;
                                })
                                .flatMap(this::process)
                                // when the flux is finished - remove this flux from context.
                                .doOnComplete(() -> cache.remove(ctx.get(CACHE_KEY_ID)))
                )
                // when we start subscribing, give this flux pipeline a request id in the context
                // we cannot update the context during the Flux pipeline, but we can use an external cache object.
                .contextWrite(ctx -> ctx.put(CACHE_KEY_ID, requestId));

        StepVerifier.create(daFlux)
                .expectNext(2)
                .expectNext(4)
                .expectNext(2)
                .expectNext(4)
                .expectNext(2)
                .then(() -> {
                    assertThat(cache).isEmpty();
                })
                .verifyComplete();

        // we calculated two values - then we used cache
        assertThat(calculatedCounter.get()).isEqualTo(2);
    }

    private Mono<Integer> process(Integer value) {
        System.out.println("PROCESSING" + value);
        return Mono.deferContextual(ctx -> {
            String requestId = ctx.get(CACHE_KEY_ID);
            Map<Integer, Integer> requestCache = null;
            if (requestId != null) {
                requestCache = cache.computeIfAbsent(requestId, reqId -> new HashMap<>());
                Integer cached = requestCache.get(value);
                if (cached != null) {
                    System.out.println("FOR [%s] using Cached [%s]\n".formatted(value, cached));
                    return Mono.just(cached);
                }
            }
            //if we'e got here - we have to calculate a value
            Integer calculated = calculate(value);
            if (requestCache != null) {
                requestCache.put(value, calculated);
            }
            return Mono.just(calculated);
        });
    }

    private Integer calculate(Integer value){
        Integer calculated = 2 * value;
        this.calculatedCounter.incrementAndGet();
        return calculated;
    }

}
