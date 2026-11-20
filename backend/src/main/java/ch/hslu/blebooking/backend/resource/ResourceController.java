package ch.hslu.blebooking.backend.resource;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.hslu.blebooking.backend.common.error.ApiException;
import ch.hslu.blebooking.backend.common.error.ErrorCode;

/**
 * Resource catalog endpoints (docs/api/booking-api.md, US-02.01). Requires authentication.
 */
@RestController
@RequestMapping("/resources")
class ResourceController {

	private final ResourceCatalog catalog;

	ResourceController(ResourceCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	List<ResourceSummary> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) ResourceType type,
			@RequestParam(required = false) String q) {
		return catalog.findVisible(jwt.getClaimAsString("role"), type, q);
	}

	@GetMapping("/{id}")
	ResourceDetails get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
		return catalog.findVisible(id, jwt.getClaimAsString("role"))
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Resource not found"));
	}

}
