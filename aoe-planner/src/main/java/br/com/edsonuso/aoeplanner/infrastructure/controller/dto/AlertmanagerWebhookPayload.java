package br.com.edsonuso.aoeplanner.infrastructure.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertmanagerWebhookPayload(
        List<Alert> alerts
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Alert(
            Map<String, String> labels,
            String status
    ) {
        public String getAlertName() {
            return labels.get("alertname");
        }
    }
}
