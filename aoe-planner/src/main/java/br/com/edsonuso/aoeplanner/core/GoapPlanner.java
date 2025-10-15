package br.com.edsonuso.aoeplanner.core;


import br.com.edsonuso.aoeplanner.model.Action;
import br.com.edsonuso.aoeplanner.model.Fact;
import br.com.edsonuso.aoeplanner.model.Goal;
import br.com.edsonuso.aoeplanner.model.Plan;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.util.*;

@Component
public class GoapPlanner {
    private static final int MAX_ITERATIONS = 1000;

    public Optional<Plan> findPlan(Set<Fact> currentState, List<Action> availableActions, Goal goal) {
        Node startNode = new Node(null, 0, currentState, null);

        // PriorityQueue ordenada por fScore (gScore + hScore)
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(node -> node.fScore));

        // Mantém os melhores gScores para cada estado
        Map<Set<Fact>, Integer> bestGScores = new HashMap<>();

        startNode.hScore = calculateHeuristic(startNode.state, goal.getDesiredState());
        startNode.fScore = startNode.gScore + startNode.hScore;

        openSet.add(startNode);
        bestGScores.put(startNode.state, startNode.gScore);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            Node currentNode = openSet.poll();

            // Verifica se atingimos o objetivo
            if (stateSatisfiesGoal(currentNode.state, goal.getDesiredState())) {
                return Optional.of(reconstructPlan(currentNode, goal));
            }

            // Expande os vizinhos
            for (Action action : availableActions) {
                if (actionIsApplicable(action, currentNode.state)) {
                    Set<Fact> neighborState = applyEffects(currentNode.state, action.getEffects());
                    int tentativeGScore = currentNode.gScore + action.getCost();

                    // Verifica se encontramos um caminho melhor para este estado
                    Integer bestKnownGScore = bestGScores.get(neighborState);
                    if (bestKnownGScore == null || tentativeGScore < bestKnownGScore) {
                        Node neighborNode = new Node(currentNode, tentativeGScore, neighborState, action);
                        neighborNode.hScore = calculateHeuristic(neighborState, goal.getDesiredState());
                        neighborNode.fScore = neighborNode.gScore + neighborNode.hScore;

                        openSet.add(neighborNode);
                        bestGScores.put(neighborState, tentativeGScore);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Verifica se o estado atual satisfaz todos os requisitos do objetivo
     */
    private boolean stateSatisfiesGoal(Set<Fact> state, Map<String, Object> desiredState) {
        Map<String, Object> stateMap = convertFactsToMap(state);

        for (Map.Entry<String, Object> entry : desiredState.entrySet()) {
            String key = entry.getKey();
            Object desiredValue = entry.getValue();
            Object actualValue = stateMap.get(key);

            if (actualValue == null || !actualValue.equals(desiredValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica se uma ação pode ser aplicada no estado atual
     */
    private boolean actionIsApplicable(Action action, Set<Fact> state) {
        Map<String, Object> stateMap = convertFactsToMap(state);
        Map<String, Object> preconditions = action.getPreconditions();

        for (Map.Entry<String, Object> entry : preconditions.entrySet()) {
            String key = entry.getKey();
            Object requiredValue = entry.getValue();
            Object actualValue = stateMap.get(key);

            if (actualValue == null || !actualValue.equals(requiredValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Aplica os efeitos de uma ação em um estado, criando um novo estado
     */
    private Set<Fact> applyEffects(Set<Fact> originalState, Map<String, Object> effects) {
        // Cria uma cópia do estado original
        Map<String, Object> newStateMap = convertFactsToMap(originalState);

        // Aplica os efeitos, se houver
        if (effects != null) {
            newStateMap.putAll(effects);
        }

        // Converte de volta para Set<Fact>
        return convertMapToFacts(newStateMap);
    }

    /**
     * Heurística: conta quantas propriedades do objetivo ainda não foram satisfeitas
     * Esta é uma heurística admissível (nunca superestima o custo real)
     */
    private int calculateHeuristic(Set<Fact> state, Map<String, Object> desiredState) {
        Map<String, Object> stateMap = convertFactsToMap(state);
        int unsatisfiedGoals = 0;

        if(desiredState != null){
            for (Map.Entry<String, Object> entry : desiredState.entrySet()) {
                String key = entry.getKey();
                Object desiredValue = entry.getValue();
                Object actualValue = stateMap.get(key);

                if (actualValue == null || !actualValue.equals(desiredValue)) {
                    unsatisfiedGoals++;
                }
            }
        }


        return unsatisfiedGoals;
    }

    /**
     * Reconstrói o plano seguindo o caminho de volta do nó final até o inicial
     */
    private Plan reconstructPlan(Node finalNode, Goal goal) {
        List<Action> actions = new ArrayList<>();
        Node current = finalNode;

        // Percorre de trás para frente
        while (current.parent != null) {
            actions.add(current.generatingAction);
            current = current.parent;
        }

        // Inverte para obter a ordem correta
        Collections.reverse(actions);

        return new Plan(goal, actions, finalNode.gScore);
    }

    /**
     * Converte Set<Fact> para Map<String, Object> para facilitar comparações
     */
    private Map<String, Object> convertFactsToMap(Set<Fact> facts) {
        Map<String, Object> map = new HashMap<>();
        for (Fact fact : facts) {
            map.put(fact.name(), fact.value());
        }
        return map;
    }

    /**
     * Converte Map<String, Object> para Set<Fact>
     */
    private Set<Fact> convertMapToFacts(Map<String, Object> map) {
        Set<Fact> facts = new HashSet<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            facts.add(new Fact(entry.getKey(), entry.getValue()));
        }
        return facts;
    }

    /**
     * Classe interna para representar um nó na busca A*
     */
    private static class Node {
        Node parent;
        int gScore; // Custo real do início até aqui
        int hScore; // Heurística (estimativa do custo daqui até o objetivo)
        int fScore; // gScore + hScore (custo total estimado)
        Set<Fact> state;
        Action generatingAction;

        Node(Node parent, int gScore, Set<Fact> state, Action generatingAction) {
            this.parent = parent;
            this.gScore = gScore;
            this.state = state;
            this.generatingAction = generatingAction;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(state, node.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(state);
        }
    }
}
