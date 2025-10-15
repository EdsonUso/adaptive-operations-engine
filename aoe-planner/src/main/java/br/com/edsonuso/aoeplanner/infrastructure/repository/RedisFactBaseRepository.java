package br.com.edsonuso.aoeplanner.infrastructure.repository;

import br.com.edsonuso.aoeplanner.application.ports.out.FactBaseRepositoryPort;
import br.com.edsonuso.aoeplanner.model.Fact;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Primary
@RequiredArgsConstructor
public class RedisFactBaseRepository implements FactBaseRepositoryPort {

    private static final String FACT_BASE_KEY = "fact-base";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Set<Fact> getCurrentFactBase() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(FACT_BASE_KEY);

        return entries.entrySet().stream()
                .map(entry -> {
                    String key = (String) entry.getKey();

                    boolean value = Boolean.parseBoolean((String) entry.getValue());
                    return new Fact(key, value);
                })
                .collect(Collectors.toSet());
    }
}
