package br.com.edsonuso.aoeplanner.infrastructure.repository;

import br.com.edsonuso.aoeplanner.application.ports.out.ActionRepositoryPort;
import br.com.edsonuso.aoeplanner.model.Action;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@Slf4j
public class YamlActionRepository implements ActionRepositoryPort {
    @Value("${planner.actions.path}")
    private String actionsPath;

    private List<Action> cachedActions;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @PostConstruct
    public void initialize() {
        log.info("Carregando definições de ações do caminho: {}", actionsPath);
        List<Action> loadedActions = new ArrayList<>();
        try {
            // Usa um resolver do Spring para encontrar todos os arquivos que correspondem ao padrão
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(actionsPath);
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    List<Action> actionsFromFile = mapper.readValue(inputStream, new TypeReference<>() {});
                    loadedActions.addAll(actionsFromFile);
                    log.debug("Carregadas {} ações de {}", actionsFromFile.size(), resource.getFilename());
                }
            }
        } catch (IOException e) {
            log.error("Falha ao carregar ou processar arquivos de ações. O planejador pode não ter ações disponíveis.", e);
        }
        this.cachedActions = Collections.unmodifiableList(loadedActions);
        log.info("Total de {} ações carregadas e prontas para uso.", this.cachedActions.size());
    }

    @Override
    public List<Action> findAll() {
        return this.cachedActions;
    }
}
