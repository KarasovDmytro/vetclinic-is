package ua.vetclinic.api.medications;

import java.util.List;

public record MedicationPage(int page, int pageSize, long total, List<MedicationResponse> items) {
}
