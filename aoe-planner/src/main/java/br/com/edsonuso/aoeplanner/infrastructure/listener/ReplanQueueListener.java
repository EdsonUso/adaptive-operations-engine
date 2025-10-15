package br.com.edsonuso.aoeplanner.infrastructure.listener;

import br.com.edsonuso.aoeplanner.application.ports.in.GeneratePlanUseCase;
import br.com.edsonuso.aoeplanner.model.Goal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReplanQueueListener {

    private final GeneratePlanUseCase generatePlanUseCase;

    @RabbitListener(queues = "aoe.replan.queue")
    public void onReplanRequest(Goal goal) {
        log.info("Solicitação de replanejamento recebida para o objetivo: {}", goal.getName());
        // Simplesmente re-executa o caso de uso de geração de plano com o mesmo objetivo.
        // O planner irá ler a Base de Fatos atualizada e gerar um novo plano.
        generatePlanUseCase.execute(goal);
    }
}
