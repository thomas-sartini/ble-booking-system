package ch.hslu.blebooking.backend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

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

}
