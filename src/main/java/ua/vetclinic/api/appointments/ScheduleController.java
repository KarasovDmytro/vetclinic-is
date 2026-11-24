package ua.vetclinic.api.appointments;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST-ресурс сітки розкладу лікаря {@code /api/v1/doctors/{doctorId}/schedule}. */
@RestController
class ScheduleController {

  private final ScheduleService scheduleService;

  ScheduleController(ScheduleService scheduleService) {
    this.scheduleService = scheduleService;
  }

  /**
   * Повертає слоти лікаря з позначкою «вільний» або «зайнятий» (операція {@code getDoctorSchedule},
   * REQ-F-020, REQ-F-026, REQ-F-049, REQ-NF-004).
   *
   * @param doctorId лікар
   * @param from перший день (включно)
   * @param to останній день (включно), не більше 7 днів від {@code from}
   * @return сітка слотів, відсортованих за часом
   */
  @GetMapping("/api/v1/doctors/{doctorId}/schedule")
  ScheduleResponse getDoctorSchedule(
      @PathVariable UUID doctorId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
    return scheduleService.schedule(doctorId, from, to);
  }
}
