package ua.vetclinic.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

public record ErrorDetail(
    String field,
    String reason,
    String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> params) {

  public ErrorDetail(String field, String reason, String message) {
    this(field, reason, message, null);
  }
}
