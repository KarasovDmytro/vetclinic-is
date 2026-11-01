package ua.vetclinic.api.visits;

import java.util.List;

public record VisitPage(int page, int pageSize, long total, List<VisitResponse> items) {
}
