package br.com.edsonuso.aoeplanner.infrastructure.repository;

import br.com.edsonuso.aoeplanner.application.ports.out.FactBaseRepositoryPort;
import br.com.edsonuso.aoeplanner.model.Fact;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

@Repository
@Profile("test")
public class InMemoryFactBaseRepository implements FactBaseRepositoryPort {


    @Override
    public Set<Fact> getCurrentFactBase() {
        Set<Fact> facts = new HashSet<>();
        facts.add(new Fact("service_web_healthy", false));
        facts.add(new Fact("port_9090_in_use", true));
        return facts;
    }
}
