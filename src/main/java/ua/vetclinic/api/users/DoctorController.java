package ua.vetclinic.api.users;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors")
class DoctorController {

	record DoctorResponse(UUID id, String fullName) {
	}

	private final Doctors doctors;

	DoctorController(Doctors doctors) {
		this.doctors = doctors;
	}

	@GetMapping
	List<DoctorResponse> listDoctors() {
		return doctors.listActive().stream().map(doctor -> new DoctorResponse(doctor.id(), doctor.fullName()))
				.toList();
	}
}
