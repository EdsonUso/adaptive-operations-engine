package br.com.edsonuso.aoeplanner.infrastructure.controller;

import br.com.edsonuso.aoeplanner.application.ports.in.GeneratePlanUseCase;
import br.com.edsonuso.aoeplanner.model.Goal;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GeneratePlanUseCase generatePlanUseCase;
    private static final Logger log = LoggerFactory.getLogger(GoalController.class);

    @PostMapping
    public ResponseEntity<Void> submitGoal(@RequestBody Goal goal){
        log.info("Received goal: {}", goal);
        generatePlanUseCase.execute(goal);
        return ResponseEntity.accepted().build();
    }
}
