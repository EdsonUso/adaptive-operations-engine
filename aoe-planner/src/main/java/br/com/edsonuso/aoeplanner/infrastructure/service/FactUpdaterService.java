package br.com.edsonuso.aoeplanner.infrastructure.service;

import br.com.edsonuso.aoeplanner.application.ports.out.FactBaseRepositoryPort;
import br.com.edsonuso.aoeplanner.model.Fact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class FactUpdaterService {

    private final FactBaseRepositoryPort factBaseRepository;

    public void persistFacts(Set<Fact> facts) {
        if (facts == null || facts.isEmpty()) {
            return;
        }
        log.info("Persistindo {} fatos na base de fatos.", facts.size());
        factBaseRepository.updateFactBase(facts);
    }
}
