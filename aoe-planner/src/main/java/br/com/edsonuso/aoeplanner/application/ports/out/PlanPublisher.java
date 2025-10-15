package br.com.edsonuso.aoeplanner.application.ports.out;

import br.com.edsonuso.aoeplanner.model.Plan;

public interface PlanPublisher {
    void publish(Plan plan);
}
