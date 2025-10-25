package br.com.edsonuso.aoeplanner.infrastructure.controller;

import br.com.edsonuso.aoeplanner.application.ports.out.ActionRepositoryPort;
import br.com.edsonuso.aoeplanner.application.ports.out.FactBaseRepositoryPort;
import br.com.edsonuso.aoeplanner.application.ports.out.PlanPublisher;
import br.com.edsonuso.aoeplanner.model.Action;
import br.com.edsonuso.aoeplanner.model.Fact;
import br.com.edsonuso.aoeplanner.model.Goal;
import br.com.edsonuso.aoeplanner.model.Plan;
import br.com.edsonuso.aoeplanner.model.PlanDispatchPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Instanciando diretamente para evitar problemas de injeção de dependência no teste.
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Usamos @MockBean para substituir os beans no contexto da aplicação do Spring.
    @MockBean
    private FactBaseRepositoryPort factBaseRepository;

    @MockBean
    private ActionRepositoryPort actionRepository;

    @MockBean
    private PlanPublisher planPublisher;

    @Test
    void whenGoalIsPosted_thenPlannerGeneratesAndPublishesCorrectPlan() throws Exception {
        // Arrange: Configurar o ambiente de teste

        // 1. Definir o estado inicial do mundo (fatos)
        Set<Fact> initialFacts = Set.of(new Fact("service_web_healthy", false));
        when(factBaseRepository.getCurrentFactBase()).thenReturn(initialFacts);

        // 2. Definir as ações que o planejador pode usar
        // A ordem correta é: name, preconditions, effects, cost, executorInfo
        Action restartAction = new Action("RestartWebService", Collections.emptyMap(), Map.of("service_web_healthy", true), 1, null);
        when(actionRepository.findAll()).thenReturn(List.of(restartAction));

        // 3. Definir o objetivo que queremos alcançar
        Goal goal = new Goal(
                "ensure-service-healthy",
                1,
                Map.of("service_web_healthy", true)
        );

        // 4. Preparar um "capturador" para pegar o payload que será publicado
        ArgumentCaptor<PlanDispatchPayload> payloadCaptor = ArgumentCaptor.forClass(PlanDispatchPayload.class);

        // Act: Executar a ação a ser testada
        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goal)))
                .andExpect(status().isAccepted());

        // Assert: Verificar os resultados

        // 1. Verificar se o método publish foi chamado no nosso publisher mockado
        verify(planPublisher).publish(payloadCaptor.capture());
        Plan publishedPlan = payloadCaptor.getValue().plan();

        // 2. Verificar se o plano gerado está correto
        assertNotNull(publishedPlan, "O plano publicado não deve ser nulo.");
        assertEquals(1, publishedPlan.steps().size(), "O plano deve conter exatamente um passo.");
        assertEquals("RestartWebService", publishedPlan.steps().get(0).getName(), "O passo gerado deve ser 'RestartWebService'.");
    }

    @Test
    void whenGoalRequiresMultiStepPlan_thenPlannerGeneratesCorrectSequence() throws Exception {
        // Arrange: Configurar um cenário mais complexo

        // 1. Estado inicial: serviço fora do ar E a porta está em uso.
        Set<Fact> initialFacts = Set.of(
                new Fact("service_web_healthy", false),
                new Fact("port_9090_in_use", true)
        );
        when(factBaseRepository.getCurrentFactBase()).thenReturn(initialFacts);

        // 2. Ações disponíveis, modeladas a partir do web_service_actions.yml
        Action killBlockerAction = new Action("KillProcessBlockingPort", Map.of("port_9090_in_use", true), Map.of("port_9090_in_use", false), 8, null);
        Action restartAction = new Action("RestartWebService", Map.of("service_web_healthy", false, "port_9090_in_use", false), Map.of("service_web_healthy", true), 5, null);
        when(actionRepository.findAll()).thenReturn(List.of(killBlockerAction, restartAction));

        // 3. Objetivo final é o mesmo: serviço saudável.
        Goal goal = new Goal(
                "ensure-service-healthy",
                1,
                Map.of("service_web_healthy", true)
        );

        ArgumentCaptor<PlanDispatchPayload> payloadCaptor = ArgumentCaptor.forClass(PlanDispatchPayload.class);

        // Act
        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goal)))
                .andExpect(status().isAccepted());

        // Assert
        verify(planPublisher).publish(payloadCaptor.capture());
        Plan publishedPlan = payloadCaptor.getValue().plan();

        assertNotNull(publishedPlan, "O plano não deve ser nulo.");
        assertEquals(2, publishedPlan.steps().size(), "O plano deve conter exatamente dois passos.");
        assertEquals("KillProcessBlockingPort", publishedPlan.steps().get(0).getName(), "O primeiro passo deve ser para liberar a porta.");
        assertEquals("RestartWebService", publishedPlan.steps().get(1).getName(), "O segundo passo deve ser para reiniciar o serviço.");
    }
}