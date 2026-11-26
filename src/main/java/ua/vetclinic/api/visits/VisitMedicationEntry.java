package ua.vetclinic.api.visits;

import jakarta.persistence.Embeddable;
import java.util.UUID;

/**
 * Витрачений препарат у прийомі: назва зберігається на момент прийому, щоб історія не змінювалась
 * при перейменуванні.
 */
@Embeddable
record VisitMedicationEntry(UUID medicationId, String medicationName, int quantity) {}
