package br.com.edsonuso.aoeplanner.infrastructure.repository;

import br.com.edsonuso.aoeplanner.application.ports.out.FactBaseRepositoryPort;
import br.com.edsonuso.aoeplanner.model.Fact;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("test")
public class InMemoryFactBaseRepository implements FactBaseRepositoryPort {

    private final Map<String, Fact> factBase = new ConcurrentHashMap<>();

    @Override
    public Set<Fact> getCurrentFactBase() {
        return new HashSet<>(factBase.values());
    }

    @Override
    public void updateFactBase(Set<Fact> facts) {
        facts.forEach(fact -> factBase.put(fact.name(), fact));
    }
}
