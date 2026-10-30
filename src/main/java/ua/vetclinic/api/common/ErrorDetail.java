package ua.vetclinic.api.common;

public record ErrorDetail(String field, String reason, String message) {
}
