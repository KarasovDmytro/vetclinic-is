package ua.vetclinic.api.owners;

import java.util.List;

public record OwnerPage(int page, int pageSize, long total, List<OwnerResponse> items) {
}
