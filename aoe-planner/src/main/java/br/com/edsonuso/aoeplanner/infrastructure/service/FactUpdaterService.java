package br.com.edsonuso.aoeplanner.infrastructure.service;

import br.com.edsonuso.aoeplanner.infrastructure.controller.dto.AlertmanagerWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactUpdaterService {

    private static final String FACT_BASE_KEY = "fact-base";
    private final RedisTemplate<String, String> redisTemplate;

    public void updateFactsFromAlert(AlertmanagerWebhookPayload.Alert alert) {
        String alertName = alert.getAlertName();
        boolean isFiring = "firing".equals(alert.status());
        log.info("Atualizando fact-base a partir do alerta: {} (status: {})", alertName, alert.status());

        String factKey = null;
        String factValue = null;

        switch (alertName) {
            case "TargetAppDown":
                factKey = "service_web_healthy";
                factValue = isFiring ? "false" : "true";
                break;
            case "Port9090Blocked":
                factKey = "port_9090_in_use";
                factValue = isFiring ? "true" : "false";
                break;
            default:
                log.warn("Alerta não reconhecido recebido: {}", alertName);
                return;
        }

        if (factKey != null) {
            log.debug("Atualizando fato no Redis: {} -> {}", factKey, factValue);
            redisTemplate.opsForHash().put(FACT_BASE_KEY, factKey, factValue);
        }
    }
}
