package br.com.edsonuso.aoeplanner.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Data
@NoArgsConstructor
@Getter
@Setter
public class Action {

    private String name;
    private Map<String, Object> preconditions;
    private Map<String, Object> effects;
    private int cost;
    private ExecutorInfo executor;

    /**
     *
     * Contém as informações de como o Executor deve invocar esta ação;
     * Este é o "contrato" entre o planner e o Executor.
     */
    @Data
    @NoArgsConstructor
    public static class ExecutorInfo {
        private String type;
        private String method;
        private String url;
        private Map<String, String> headers;
        private String body;
    }

}
