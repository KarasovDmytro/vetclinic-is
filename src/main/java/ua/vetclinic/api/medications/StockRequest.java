package ua.vetclinic.api.medications;

import java.util.UUID;

/** Скільки одиниць препарату потрібно списати. */
public record StockRequest(UUID medicationId, int quantity) {
}
