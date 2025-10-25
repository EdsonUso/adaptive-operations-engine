package br.com.edsonuso.aoeplanner.infrastructure.controller;

import br.com.edsonuso.aoeplanner.application.ports.in.GeneratePlanUseCase;
import br.com.edsonuso.aoeplanner.infrastructure.controller.dto.AlertmanagerWebhookPayload;
import br.com.edsonuso.aoeplanner.infrastructure.service.DeclarativeAlertMapper;
import br.com.edsonuso.aoeplanner.infrastructure.service.FactUpdaterService;
import br.com.edsonuso.aoeplanner.model.Fact;
import br.com.edsonuso.aoeplanner.model.Goal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private static final Logger log = LoggerFactory.getLogger(AlertController.class);
    private final FactUpdaterService factUpdaterService;
    private final DeclarativeAlertMapper declarativeAlertMapper;
    private final GeneratePlanUseCase generatePlanUseCase;

    @PostMapping
    public ResponseEntity<Void> receiveAlert(@RequestBody AlertmanagerWebhookPayload payload) {
        log.info("Webhook do Alertmanager recebido com {} alerta(s)", payload.alerts().size());

        payload.alerts().forEach(alert -> {
            Set<Fact> facts = declarativeAlertMapper.map(alert);
            factUpdaterService.persistFacts(facts);

            // TODO: A criação de Goal deveria ser mais flexível
            if ("APIServiceDown".equals(alert.labels().get("alertname"))) {
                Goal goal = new Goal(
                        "restore-web-service-via-alert",
                        1,
                        Map.of("service_web_healthy", true)
                );
                generatePlanUseCase.execute(goal);
            }
        });

        return ResponseEntity.ok().build();
    }
}
