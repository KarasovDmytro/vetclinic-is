package ua.vetclinic.api.users;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-ресурс довідника лікарів {@code /api/v1/doctors}. */
@RestController
@RequestMapping("/api/v1/doctors")
class DoctorController {

  /**
   * Лікар у довіднику.
   *
   * @param id ідентифікатор користувача-лікаря
   * @param fullName ПІБ
   */
  record DoctorResponse(UUID id, String fullName) {}

  private final Doctors doctors;

  DoctorController(Doctors doctors) {
    this.doctors = doctors;
  }

  /**
   * Повертає активних лікарів, відсортованих за ПІБ (операція {@code listDoctors}, REQ-F-020,
   * REQ-F-021).
   *
   * @return активні лікарі
   */
  @GetMapping
  List<DoctorResponse> listDoctors() {
    return doctors.listActive().stream()
        .map(doctor -> new DoctorResponse(doctor.id(), doctor.fullName()))
        .toList();
  }
}
