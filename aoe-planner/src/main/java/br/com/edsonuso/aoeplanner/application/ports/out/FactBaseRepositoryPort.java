package br.com.edsonuso.aoeplanner.application.ports.out;

import br.com.edsonuso.aoeplanner.model.Fact;

import java.util.Set;

public interface FactBaseRepositoryPort {
    Set<Fact> getCurrentFactBase();
}
