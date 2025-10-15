package br.com.edsonuso.aoeplanner.application.usecase;

import br.com.edsonuso.aoeplanner.core.GoapPlanner;
import br.com.edsonuso.aoeplanner.application.ports.in.GeneratePlanUseCase;
import br.com.edsonuso.aoeplanner.application.ports.out.ActionRepositoryPort;
import br.com.edsonuso.aoeplanner.application.ports.out.FactBaseRepositoryPort;
import br.com.edsonuso.aoeplanner.application.ports.out.PlanPublisher;
import br.com.edsonuso.aoeplanner.model.Action;
import br.com.edsonuso.aoeplanner.model.Fact;
import br.com.edsonuso.aoeplanner.model.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class GeneratePlanUseCaseImpl implements GeneratePlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(GeneratePlanUseCaseImpl.class);

    private final FactBaseRepositoryPort factPort;
    private final ActionRepositoryPort  actionPort;
    private final PlanPublisher planPublisher;
    private final GoapPlanner planner;

    public GeneratePlanUseCaseImpl(FactBaseRepositoryPort factPort, ActionRepositoryPort actionPort, PlanPublisher planPublisher, GoapPlanner planner) {
        this.factPort = factPort;
        this.actionPort = actionPort;
        this.planPublisher = planPublisher;
        this.planner = planner;
    }

    @Override
    public void execute(Goal goal) {
        log.info("Iniciando processo de planejamento para o objetivo: {}", goal.getName());

        Set<Fact> currentFacts = factPort.getCurrentFactBase();
        List<Action> avaliableActions = actionPort.findAll();
        log.debug("Avaliable actions: {}", avaliableActions);
        log.debug("Avaliable Facts: {}", currentFacts);

        planner.findPlan(currentFacts, avaliableActions,goal)
                .ifPresentOrElse(
                        plan -> {
                            log.info("Plano encontrado com {} passo(s). Publicando...", plan.steps().size());
                            planPublisher.publish(plan);
                        },
                        () ->  {
                            log.warn("Nenhum plano encontrado para o objetivo: {}", goal.getName());
                        }
                );

    }
}
