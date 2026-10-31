package ua.vetclinic.api.appointments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-NF-004: тижневий розклад завантажується швидше за 2 с навіть при великій історії записів. */
@ApiIntegrationTest
@Transactional
class SchedulePerformanceTest {

	private static final Duration LIMIT = Duration.ofSeconds(2);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String insertDoctor(String fullName) {
		String doctorId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
				INSERT INTO users (id, email, full_name, role, active, created_at, password_hash)
				VALUES (?::uuid, ?, ?, 'VETERINARIAN', TRUE, now(), 'x')
				""", doctorId, "perf." + doctorId + "@clinic.example", fullName);
		return doctorId;
	}

	private String insertPatient() {
		String ownerId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
				INSERT INTO owners (id, full_name, phone, email, address, created_at, updated_at)
				VALUES (?::uuid, 'Власник Продуктивності', '+380501234699', NULL, NULL, now(), now())
				""", ownerId);
		String patientId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
				INSERT INTO patients (id, owner_id, name, species, breed, sex, birth_date, status, created_at, updated_at)
				VALUES (?::uuid, ?::uuid, 'Пацієнт Продуктивності', 'Кіт', NULL, 'UNKNOWN', NULL, 'ACTIVE', now(), now())
				""", patientId, ownerId);
		return patientId;
	}

	@Test
	@DisplayName("REQ-NF-004: тижневий розклад із 126 зайнятими слотами та 10 000 записів історії інших лікарів — швидше за 2 с")
	void weeklySchedule_isFasterThanTwoSeconds() throws Exception {
		// Arrange
		String doctorId = insertDoctor("Продуктивність Лікар");
		String historyDoctor = insertDoctor("Продуктивність Історія");
		String patientId = insertPatient();
		jdbcTemplate.update("""
				INSERT INTO appointments (id, doctor_id, patient_id, starts_at, ends_at, status, visit_id, created_at,
				                          updated_at)
				SELECT gen_random_uuid(), ?::uuid, ?::uuid,
				       ('2026-12-07 07:00:00+00'::timestamptz + (d * interval '1 day') + (s * interval '30 minutes')),
				       ('2026-12-07 07:30:00+00'::timestamptz + (d * interval '1 day') + (s * interval '30 minutes')),
				       'SCHEDULED', NULL, now(), now()
				FROM generate_series(0, 6) AS d, generate_series(0, 17) AS s
				""", doctorId, patientId);
		jdbcTemplate.update("""
				INSERT INTO appointments (id, doctor_id, patient_id, starts_at, ends_at, status, visit_id, created_at,
				                          updated_at)
				SELECT gen_random_uuid(), ?::uuid, ?::uuid,
				       ('2020-01-01 07:00:00+00'::timestamptz + (g * interval '30 minutes')),
				       ('2020-01-01 07:30:00+00'::timestamptz + (g * interval '30 minutes')),
				       'COMPLETED', NULL, now(), now()
				FROM generate_series(1, 10000) AS g
				""", historyDoctor, patientId);

		// Act
		long startedAt = System.nanoTime();
		var response = mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule")
				.param("from", "2026-12-07").param("to", "2026-12-13").with(user("admin").roles("ADMIN")));
		Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots", hasSize(126)))
				.andExpect(jsonPath("$.slots[?(@.status == 'BOOKED')]", hasSize(126)));
		assertThat(elapsed.toMillis(), lessThan(LIMIT.toMillis()));
		assertThat(response.andReturn().getResponse().getStatus(), equalTo(200));
	}
}
