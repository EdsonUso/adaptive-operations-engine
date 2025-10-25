package br.com.edsonuso.aoeplanner.infrastructure.repository;

import br.com.edsonuso.aoeplanner.application.ports.out.FactBaseRepositoryPort;
import br.com.edsonuso.aoeplanner.model.Fact;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Profile("!test")
@RequiredArgsConstructor
public class RedisFactBaseRepository implements FactBaseRepositoryPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String FACT_KEY_PREFIX = "fact:";

    @Override
    public Set<Fact> getCurrentFactBase() {
        Set<String> keys = redisTemplate.keys(FACT_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        return keys.stream()
                .map(key -> {
                    Object rawValue = redisTemplate.opsForValue().get(key);
                    Object typedValue = rawValue;
                    if (rawValue instanceof String s) {
                        if (s.equalsIgnoreCase("true")) {
                            typedValue = true;
                        } else if (s.equalsIgnoreCase("false")) {
                            typedValue = false;
                        }
                    }
                    return new Fact(key.substring(FACT_KEY_PREFIX.length()), typedValue);
                })
                .collect(Collectors.toSet());
    }

    @Override
    public void updateFactBase(Set<Fact> facts) {
        facts.forEach(fact -> redisTemplate.opsForValue().set(FACT_KEY_PREFIX + fact.name(), fact.value()));
    }
}
