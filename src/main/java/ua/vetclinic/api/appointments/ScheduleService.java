package ua.vetclinic.api.appointments;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import ua.vetclinic.api.common.ClinicZone;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.RequestValidationException;
import ua.vetclinic.api.common.ResourceNotFoundException;
import ua.vetclinic.api.patients.PatientLookup;
import ua.vetclinic.api.users.Doctors;

@Service
class ScheduleService {

	private static final long MAX_RANGE_DAYS = 7;

	private final Doctors doctors;

	private final AppointmentRepository appointmentRepository;

	private final PatientLookup patients;

	ScheduleService(Doctors doctors, AppointmentRepository appointmentRepository, PatientLookup patients) {
		this.doctors = doctors;
		this.appointmentRepository = appointmentRepository;
		this.patients = patients;
	}

	ScheduleResponse schedule(UUID doctorId, LocalDate from, LocalDate to) {
		if (to.isBefore(from)) {
			throw new RequestValidationException(
					List.of(new ErrorDetail("to", "INVALID_RANGE", "Дата завершення раніше за дату початку")));
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
			throw new RequestValidationException(
					List.of(new ErrorDetail("to", "RANGE_TOO_LARGE", "Діапазон більший за 7 днів")));
		}
		doctors.find(doctorId).orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
		NavigableMap<Instant, SlotAppointment> occupied = occupiedSlots(doctorId, from, to);
		List<SlotResponse> slots = new ArrayList<>();
		for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
			slotStartsOf(day, occupied).forEach(start -> {
				SlotAppointment appointment = occupied.get(start.toInstant());
				slots.add(new SlotResponse(start.toOffsetDateTime(),
						start.plus(ClinicSchedule.SLOT).toOffsetDateTime(),
						appointment == null ? SlotStatus.FREE : SlotStatus.BOOKED, appointment));
			});
		}
		return new ScheduleResponse(doctorId, from, to, (int) ClinicSchedule.SLOT.toMinutes(), slots);
	}

	/** У робочий день усі слоти сітки, у вихідний лише ті, що зайняті записами, які вже там стоять (AD-25.6-7). */
	private static List<ZonedDateTime> slotStartsOf(LocalDate day, NavigableMap<Instant, SlotAppointment> occupied) {
		if (ClinicSchedule.isWorkingDay(day)) {
			return ClinicSchedule.slotStartsOf(day);
		}
		Instant dayStart = day.atStartOfDay(ClinicZone.ZONE).toInstant();
		Instant nextDayStart = day.plusDays(1).atStartOfDay(ClinicZone.ZONE).toInstant();
		return occupied.subMap(dayStart, true, nextDayStart, false).keySet().stream()
				.map(start -> start.atZone(ClinicZone.ZONE)).toList();
	}

	private NavigableMap<Instant, SlotAppointment> occupiedSlots(UUID doctorId, LocalDate from, LocalDate to) {
		OffsetDateTime rangeStart = from.atStartOfDay(ClinicZone.ZONE).toOffsetDateTime();
		OffsetDateTime rangeEnd = to.plusDays(1).atStartOfDay(ClinicZone.ZONE).toOffsetDateTime();
		List<AppointmentEntity> appointments = appointmentRepository.findInRange(doctorId, rangeStart, rangeEnd,
				AppointmentService.OCCUPYING);
		Map<UUID, String> names = patients.namesOf(appointments.stream().map(AppointmentEntity::patientId).toList());
		NavigableMap<Instant, SlotAppointment> occupied = new TreeMap<>();
		for (AppointmentEntity appointment : appointments) {
			occupied.put(appointment.startsAt().toInstant(), new SlotAppointment(appointment.id(),
					appointment.patientId(), names.get(appointment.patientId()), appointment.status()));
		}
		return occupied;
	}
}
