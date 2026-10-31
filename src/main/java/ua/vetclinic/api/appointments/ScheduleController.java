package ua.vetclinic.api.appointments;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ScheduleController {

	private final ScheduleService scheduleService;

	ScheduleController(ScheduleService scheduleService) {
		this.scheduleService = scheduleService;
	}

	@GetMapping("/api/v1/doctors/{doctorId}/schedule")
	ScheduleResponse getDoctorSchedule(@PathVariable UUID doctorId, @RequestParam LocalDate from,
			@RequestParam LocalDate to) {
		return scheduleService.schedule(doctorId, from, to);
	}
}
