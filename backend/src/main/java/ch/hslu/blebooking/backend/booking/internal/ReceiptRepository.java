package ch.hslu.blebooking.backend.booking.internal;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
}
