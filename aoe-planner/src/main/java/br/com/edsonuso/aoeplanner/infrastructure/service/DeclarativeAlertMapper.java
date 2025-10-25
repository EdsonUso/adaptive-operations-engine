package br.com.edsonuso.aoeplanner.infrastructure.service;

import br.com.edsonuso.aoeplanner.infrastructure.controller.dto.AlertmanagerWebhookPayload;
import br.com.edsonuso.aoeplanner.model.AlertMapping;
import br.com.edsonuso.aoeplanner.model.Fact;
import br.com.edsonuso.aoeplanner.model.FactMapping;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeclarativeAlertMapper {

    @Value("${planner.alert-mappings.path}")
    private String mappingsPath;

    private List<AlertMapping> cachedMappings;
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\{\{\s*\.(.*?)\s*\}\}");


    @PostConstruct
    public void initialize() {
        log.info("Carregando definições de mapeamento de alertas de: {}", mappingsPath);
        List<AlertMapping> loadedMappings = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(mappingsPath);
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    List<AlertMapping> mappingsFromFile = mapper.readValue(inputStream, new TypeReference<>() {});
                    loadedMappings.addAll(mappingsFromFile);
                    log.debug("Carregado {} mapeamentos de {}", mappingsFromFile.size(), resource.getFilename());
                }
            }
        } catch (IOException e) {
            log.error("Falha ao carregar ou processar arquivos de mapeamento de alertas.", e);
        }
        this.cachedMappings = Collections.unmodifiableList(loadedMappings);
        log.info("Total de {} mapeamentos de alerta carregados.", this.cachedMappings.size());
    }

    public Set<Fact> map(AlertmanagerWebhookPayload.Alert alert) {
        return cachedMappings.stream()
                .filter(mapping -> alertMatches(mapping, alert))
                .flatMap(mapping -> createFactsFromMapping(mapping, alert).stream())
                .collect(Collectors.toSet());
    }

    private boolean alertMatches(AlertMapping mapping, AlertmanagerWebhookPayload.Alert alert) {
        if (!mapping.getAlertName().equals(alert.labels().get("alertname"))) {
            return false;
        }

        if (mapping.getMatches() != null && !mapping.getMatches().isEmpty()) {
            return mapping.getMatches().entrySet().stream()
                    .allMatch(entry -> entry.getValue().equals(alert.labels().get(entry.getKey())));
        }

        return true;
    }

    private Set<Fact> createFactsFromMapping(AlertMapping mapping, AlertmanagerWebhookPayload.Alert alert) {
        return mapping.getFacts().stream()
                .map(factMapping -> {
                    String resolvedValue = resolveValue(factMapping.getValue(), alert);
                    Object typedValue = convertToTypedObject(resolvedValue);
                    return new Fact(factMapping.getName(), typedValue);
                })
                .collect(Collectors.toSet());
    }

    private String resolveValue(String valueTemplate, AlertmanagerWebhookPayload.Alert alert) {
        if (valueTemplate == null || !valueTemplate.contains("{{")) {
            return valueTemplate;
        }

        Matcher matcher = TEMPLATE_PATTERN.matcher(valueTemplate);
        if (matcher.find()) {
            String keyPath = matcher.group(1);
            String[] keys = keyPath.split("\.");
            if (keys.length == 2) {
                Map<String, String> sourceMap = getSourceMap(keys[0], alert);
                if (sourceMap != null) {
                    String resolved = sourceMap.get(keys[1]);
                    return resolved != null ? resolved : valueTemplate;
                }
            }
        }
        return valueTemplate;
    }

    private Map<String, String> getSourceMap(String sourceName, AlertmanagerWebhookPayload.Alert alert) {
        return switch (sourceName) {
            case "CommonLabels", "labels" -> alert.labels();
            case "CommonAnnotations", "annotations" -> alert.annotations();
            default -> null;
        };
    }

    private Object convertToTypedObject(String value) {
        if (value == null) return null;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Not an integer, return as string
            return value;
        }
    }
}
