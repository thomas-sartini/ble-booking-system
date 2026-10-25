package ch.hslu.blebooking.backend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Checks that the modules only use the dependencies declared in their package-info.java
 * and that there are no cycles between modules.
 */
class ModularityIT {

	private final ApplicationModules modules = ApplicationModules.of(BackendApplication.class);

	@Test
	void modulesFollowDeclaredDependencies() {
		modules.verify();
	}

	/**
	 * Writes module diagrams (PlantUML) to target/spring-modulith-docs,
	 * so they can be compared with the architecture diagram in the report.
	 */
	@Test
	void writesModuleDocumentation() {
		new Documenter(modules).writeDocumentation();
	}

}
