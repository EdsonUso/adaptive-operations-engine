package br.com.edsonuso.aoeplanner.application.ports.out;

import br.com.edsonuso.aoeplanner.model.Plan;

import br.com.edsonuso.aoeplanner.model.PlanDispatchPayload;

public interface PlanPublisher {
    void publish(PlanDispatchPayload payload);
}
