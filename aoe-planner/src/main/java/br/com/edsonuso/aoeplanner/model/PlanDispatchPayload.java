package br.com.edsonuso.aoeplanner.model;

import java.io.Serializable;

public record PlanDispatchPayload(Goal targetGoal, Plan plan) implements Serializable {
}
