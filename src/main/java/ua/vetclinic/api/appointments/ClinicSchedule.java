package ua.vetclinic.api.appointments;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ua.vetclinic.api.common.ClinicZone;
import ua.vetclinic.api.common.ErrorDetail;

/** Робочі години й сітка слотів клініки (AD-05): 09:00–18:00, слоти по 30 хвилин, усі дні тижня. */
final class ClinicSchedule {

	static final LocalTime WORK_START = LocalTime.of(9, 0);

	static final LocalTime WORK_END = LocalTime.of(18, 0);

	static final Duration SLOT = Duration.ofMinutes(30);

	private ClinicSchedule() {
	}

	/** Перевіряє, що початок збігається з межею слота і лежить у робочих годинах (AD-05.3, AD-17.5). */
	static Optional<ErrorDetail> slotError(ZonedDateTime start) {
		LocalTime time = start.toLocalTime();
		if (time.getSecond() != 0 || time.getNano() != 0 || time.getMinute() % SLOT.toMinutes() != 0) {
			return Optional.of(new ErrorDetail("startsAt", "SLOT_NOT_ALIGNED", "Початок не збігається з межею слота"));
		}
		if (time.isBefore(WORK_START) || !time.isBefore(WORK_END)) {
			return Optional.of(new ErrorDetail("startsAt", "OUTSIDE_WORKING_HOURS", "Слот поза робочими годинами"));
		}
		return Optional.empty();
	}

	static List<ZonedDateTime> slotStartsOf(LocalDate day) {
		List<ZonedDateTime> starts = new ArrayList<>();
		LocalTime time = WORK_START;
		while (time.isBefore(WORK_END)) {
			starts.add(ZonedDateTime.of(day, time, ClinicZone.ZONE));
			time = time.plus(SLOT);
		}
		return starts;
	}
}
