package br.com.edsonuso.aoeplanner.infrastructure.controller;

import br.com.edsonuso.aoeplanner.infrastructure.controller.dto.AlertmanagerWebhookPayload;
import br.com.edsonuso.aoeplanner.infrastructure.service.FactUpdaterService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private static final Logger log = LoggerFactory.getLogger(AlertController.class);
    private final FactUpdaterService factUpdaterService;

    @PostMapping
    public ResponseEntity<Void> receiveAlert(@RequestBody AlertmanagerWebhookPayload payload) {
        log.info("Webhook do Alertmanager recebido com {} alerta(s)", payload.alerts().size());

        payload.alerts().forEach(factUpdaterService::updateFactsFromAlert);

        return ResponseEntity.ok().build();
    }
}
