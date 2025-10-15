package br.com.edsonuso.aoeplanner.model;

import java.util.Objects;

/*
* Representa um fato atomico sobre o estado do ambiente.(FactBase)
* Ex: ("service_web_healthy", false), ("active_connections", 105)
*
* Usando como um record para garantir imutabilidade e simplicidade.
*
 */
public record Fact(String name, Object value) {

    public Fact {
        Objects.requireNonNull(name, "O nome do fato não pode ser nulo.");
    }
}
