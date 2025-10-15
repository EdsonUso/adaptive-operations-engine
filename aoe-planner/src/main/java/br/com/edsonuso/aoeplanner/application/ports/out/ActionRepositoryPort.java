package br.com.edsonuso.aoeplanner.application.ports.out;

import br.com.edsonuso.aoeplanner.model.Action;

import java.util.List;

public interface ActionRepositoryPort {
    List<Action> findAll();
}
