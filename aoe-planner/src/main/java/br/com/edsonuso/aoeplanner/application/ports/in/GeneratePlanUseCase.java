package br.com.edsonuso.aoeplanner.application.ports.in;

import br.com.edsonuso.aoeplanner.model.Goal;

public interface GeneratePlanUseCase {
    public void execute(Goal goal);
}
