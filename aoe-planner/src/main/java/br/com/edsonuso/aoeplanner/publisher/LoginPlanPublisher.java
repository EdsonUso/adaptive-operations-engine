package br.com.edsonuso.aoeplanner.publisher;

import br.com.edsonuso.aoeplanner.application.ports.out.PlanPublisher;
import br.com.edsonuso.aoeplanner.model.Plan;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import br.com.edsonuso.aoeplanner.model.PlanDispatchPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoginPlanPublisher implements PlanPublisher {

    @Override
    public void publish(PlanDispatchPayload payload) {
        log.info("Mock Plan Publisher: Plano para o objetivo '{}' não será publicado.", payload.targetGoal().getName());
    }
}
