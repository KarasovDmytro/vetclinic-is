package ua.vetclinic.api.users;

import java.util.List;

public record UserPage(int page, int pageSize, long total, List<UserResponse> items) {
}
