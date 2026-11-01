package ua.vetclinic.api.visits;

import java.util.UUID;

import jakarta.persistence.Embeddable;

/** Витрачений препарат у прийомі: назва зберігається на момент прийому, щоб історія не змінювалась при перейменуванні. */
@Embeddable
record VisitMedicationEntry(UUID medicationId, String medicationName, int quantity) {
}
