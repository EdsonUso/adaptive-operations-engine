package br.com.edsonuso.aoeplanner.publisher;

import br.com.edsonuso.aoeplanner.application.ports.out.PlanPublisher;
import br.com.edsonuso.aoeplanner.infrastructure.config.RabbitMQConfig;
import br.com.edsonuso.aoeplanner.model.Plan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPlanPublisher implements PlanPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(Plan plan) {
        log.info("Publicando plano gerado para a exchange '{}' com a routing key '{}'", RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, plan);
        log.info("Plano publicado com sucesso.");
    }
}
