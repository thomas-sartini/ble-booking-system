package ch.hslu.blebooking.backend.resource.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ch.hslu.blebooking.backend.resource.ResourceStatus;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

	List<Resource> findByStatus(ResourceStatus status);

	boolean existsByName(String name);

}
