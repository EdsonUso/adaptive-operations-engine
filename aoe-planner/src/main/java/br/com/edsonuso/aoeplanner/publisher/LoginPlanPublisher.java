package br.com.edsonuso.aoeplanner.publisher;

import br.com.edsonuso.aoeplanner.application.ports.out.PlanPublisher;
import br.com.edsonuso.aoeplanner.model.Plan;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
@RequiredArgsConstructor
@Slf4j
public class LoginPlanPublisher implements PlanPublisher {

    private final ObjectMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public void publish(Plan plan) {
        String planAsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(plan);
        log.info("--- PLANO GERADO E 'PUBLICADO' ---");
        log.info(planAsJson);
        log.info("---------------------------------");
    }
}
