package ch.hslu.blebooking.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checks the layer rules inside a module: controller, service, repository.
 * The rules also pass while a layer is still empty, so they are already active for the first feature.
 */
class ArchitectureIT {

	private static final JavaClasses CLASSES = new ClassFileImporter()
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.importPackages("ch.hslu.blebooking.backend");

	@Test
	void controllersDoNotUseRepositories() {
		noClasses().that().areAnnotatedWith(RestController.class)
			.should().dependOnClassesThat().areAssignableTo(Repository.class)
			.allowEmptyShould(true)
			.check(CLASSES);
	}

	@Test
	void controllersDoNotExposeEntities() {
		noClasses().that().areAnnotatedWith(RestController.class)
			.should().dependOnClassesThat().areAnnotatedWith(Entity.class)
			.allowEmptyShould(true)
			.check(CLASSES);
	}

	@Test
	void repositoriesAreOnlyUsedByServices() {
		classes().that().areAssignableTo(Repository.class)
			.should().onlyBeAccessed().byClassesThat().areAnnotatedWith(Service.class)
			.allowEmptyShould(true)
			.check(CLASSES);
	}

}
