package br.com.edsonuso.aoeplanner;

import br.com.edsonuso.aoeplanner.application.ports.in.GeneratePlanUseCase;
import br.com.edsonuso.aoeplanner.application.ports.out.PlanPublisher;
import br.com.edsonuso.aoeplanner.model.Goal;
import br.com.edsonuso.aoeplanner.model.Plan;
import br.com.edsonuso.aoeplanner.model.PlanDispatchPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class FullScenarioTest {

    @Autowired
    private GeneratePlanUseCase generatePlanUseCase;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private PlanPublisher planPublisher;

    private static final String FACT_BASE_KEY = "fact-base";

    @BeforeEach
    void setUp() {
        // Limpa a base de fatos antes de cada teste
        redisTemplate.delete(FACT_BASE_KEY);
    }

    @Test
    void givenMaliciousContainer_whenGoalIsNeutralize_thenPlanIsRemoveContainer() {
        // 1. Arrange: Definir o estado do mundo (Fact Base)
        // Simula o script definindo o fato no Redis
        redisTemplate.opsForHash().put(FACT_BASE_KEY, "malicious_containers_present", "true");

        // Verifica se o fato foi realmente definido
        String factValue = (String) redisTemplate.opsForHash().get(FACT_BASE_KEY, "malicious_containers_present");
        assertEquals("true", factValue, "O fato no Redis não foi definido corretamente antes do teste.");

        // 2. Arrange: Definir o objetivo
        Goal goal = new Goal(
                "neutralize-threat",
                1,
                Map.of("malicious_containers_present", false)
        );

        // 3. Act: Executar o caso de uso
        generatePlanUseCase.execute(goal);

        // 4. Assert: Verificar o plano publicado
        ArgumentCaptor<PlanDispatchPayload> payloadCaptor = ArgumentCaptor.forClass(PlanDispatchPayload.class);
        verify(planPublisher).publish(payloadCaptor.capture());
        Plan publishedPlan = payloadCaptor.getValue().plan();

        assertNotNull(publishedPlan, "O plano publicado não deve ser nulo.");
        assertFalse(publishedPlan.steps().isEmpty(), "O plano não deve estar vazio.");
        assertEquals(1, publishedPlan.steps().size(), "O plano deve conter exatamente um passo.");
        assertEquals("RemoveMaliciousContainers", publishedPlan.steps().get(0).getName(), "A ação planejada deve ser 'RemoveMaliciousContainers'.");
    }
}
