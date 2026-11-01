package ua.vetclinic.api.patients;

import java.util.List;

public record PatientPage(int page, int pageSize, long total, List<PatientResponse> items) {
}
