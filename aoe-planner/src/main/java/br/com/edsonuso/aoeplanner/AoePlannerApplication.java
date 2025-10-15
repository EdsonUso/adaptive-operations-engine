package br.com.edsonuso.aoeplanner;

import br.com.edsonuso.aoeplanner.application.ports.in.GeneratePlanUseCase;
import br.com.edsonuso.aoeplanner.application.ports.out.ActionRepositoryPort;
import br.com.edsonuso.aoeplanner.application.ports.out.FactBaseRepositoryPort;
import br.com.edsonuso.aoeplanner.application.ports.out.PlanPublisher;
import br.com.edsonuso.aoeplanner.application.usecase.GeneratePlanUseCaseImpl;
import br.com.edsonuso.aoeplanner.core.GoapPlanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AoePlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AoePlannerApplication.class, args);
	}

	@Bean
	public GeneratePlanUseCase generatePlanUseCase(

			FactBaseRepositoryPort factPort,
			ActionRepositoryPort actionPort,
			PlanPublisher planPublisher,
			GoapPlanner planner
	) {
		return new GeneratePlanUseCaseImpl(factPort, actionPort, planPublisher, planner);
	}
}