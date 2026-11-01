package ua.vetclinic.api.patients;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
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

/** REQ-NF-003: пошук за іменем не повільніший за 3 с при 10 000 пацієнтів. */
@ApiIntegrationTest
@Transactional
class PatientSearchPerformanceTest {

	private static final Duration LIMIT = Duration.ofSeconds(3);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String insertOwner(String fullName, String phone) {
		String ownerId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
				INSERT INTO owners (id, full_name, phone, email, address, created_at, updated_at)
				VALUES (?::uuid, ?, ?, NULL, NULL, now(), now())
				""", ownerId, fullName, phone);
		return ownerId;
	}

	private void insertTenThousandPatients(String ownerId) {
		jdbcTemplate.update("""
				INSERT INTO patients (id, owner_id, name, species, breed, sex, birth_date, status, created_at, updated_at)
				SELECT gen_random_uuid(), ?::uuid, 'Навантажувальний Пацієнт ' || g, 'Кіт', NULL, 'UNKNOWN', NULL,
				       'ACTIVE', now(), now()
				FROM generate_series(1, 10000) AS g
				""", ownerId);
	}

	@Test
	@DisplayName("REQ-NF-003: пошук пацієнтів за підрядком серед 10 000 карток виконується швидше за 3 с")
	void searchAmongTenThousandPatients_isFasterThanThreeSeconds() throws Exception {
		// Arrange
		String ownerId = insertOwner("Власник Навантаження", "+380501234640");
		insertTenThousandPatients(ownerId);

		// Act
		long startedAt = System.nanoTime();
		var response = mockMvc.perform(get("/api/v1/patients")
				.param("q", "ПАЦІЄНТ 99")
				.with(user("admin").roles("ADMIN")));
		Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", greaterThan(0)));
		assertThat(elapsed.toMillis(), lessThan(LIMIT.toMillis()));
	}

	@Test
	@DisplayName("REQ-NF-003: пошук з фільтром власника серед 10 000 карток виконується швидше за 3 с")
	void searchWithOwnerFilterAmongTenThousandPatients_isFasterThanThreeSeconds() throws Exception {
		// Arrange
		String ownerId = insertOwner("Власник Навантаження Два", "+380501234641");
		insertTenThousandPatients(ownerId);

		// Act
		long startedAt = System.nanoTime();
		var response = mockMvc.perform(get("/api/v1/patients")
				.param("ownerId", ownerId)
				.param("q", "пацієнт")
				.param("pageSize", "100")
				.with(user("admin").roles("ADMIN")));
		Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", greaterThan(9999)));
		assertThat(elapsed.toMillis(), lessThan(LIMIT.toMillis()));
	}
}
