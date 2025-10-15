package br.com.edsonuso.aoeplanner.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    /**
     *
     * Representa um objetivo que de negócio a ser alcançado pelo sistema.
     * Define o estado final desejado da base de fatos (FactBase)
     */

    private String name;
    private int priority;
    private Map<String, Object> desiredState;
}
