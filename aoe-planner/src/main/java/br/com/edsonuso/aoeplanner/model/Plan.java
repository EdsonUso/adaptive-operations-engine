package br.com.edsonuso.aoeplanner.model;


import java.util.List;
import java.util.Objects;

/**
 *
 * Representa o resultado do planejamento. Uma sequencia ordenada de ações.
 *
 * Usado como um record para garantir imutabilidade.
 */
public record Plan(Goal targetGoal, List<Action> steps, int totalCost) {

    public Plan {
        Objects.requireNonNull(targetGoal);
        Objects.requireNonNull(steps);
        if (totalCost < 0) {
            throw new IllegalArgumentException("O custo total não pode ser negativo.");
        }
    }
}