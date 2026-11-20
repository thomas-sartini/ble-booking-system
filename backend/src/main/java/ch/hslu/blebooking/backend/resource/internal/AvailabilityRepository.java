package ch.hslu.blebooking.backend.resource.internal;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

	List<Availability> findByResourceIdIn(Collection<Long> resourceIds);

	List<Availability> findByResourceId(Long resourceId);

}
