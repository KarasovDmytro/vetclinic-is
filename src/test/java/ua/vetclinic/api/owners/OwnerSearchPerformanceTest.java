package ua.vetclinic.api.owners;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-NF-003: пошук за іменем не повільніший за 3 с при 10 000 записів. */
@ApiIntegrationTest
@Transactional
class OwnerSearchPerformanceTest {

	private static final Duration LIMIT = Duration.ofSeconds(3);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("REQ-NF-003: пошук власників за підрядком серед 10 000 записів виконується швидше за 3 с")
	void searchAmongTenThousandOwners_isFasterThanThreeSeconds() throws Exception {
		// Arrange
		jdbcTemplate.update("""
				INSERT INTO owners (id, full_name, phone, email, address, created_at, updated_at)
				SELECT gen_random_uuid(), 'Навантажувальний Власник ' || g, '+38050' || lpad(g::text, 7, '0'),
				       NULL, NULL, now(), now()
				FROM generate_series(1, 10000) AS g
				""");

		// Act
		long startedAt = System.nanoTime();
		var response = mockMvc.perform(get("/api/v1/owners")
				.param("q", "ВЛАСНИК 99")
				.with(user("admin").roles("ADMIN")));
		Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", greaterThan(0)));
		assertThat(elapsed.toMillis(), lessThan(LIMIT.toMillis()));
	}

	@Test
	@DisplayName("REQ-NF-003: перша сторінка списку без фільтра серед 10 000 записів виконується швидше за 3 с")
	void listingAmongTenThousandOwners_isFasterThanThreeSeconds() throws Exception {
		// Arrange
		jdbcTemplate.update("""
				INSERT INTO owners (id, full_name, phone, email, address, created_at, updated_at)
				SELECT gen_random_uuid(), 'Список Власник ' || g, '+38067' || lpad(g::text, 7, '0'),
				       NULL, NULL, now(), now()
				FROM generate_series(1, 10000) AS g
				""");

		// Act
		long startedAt = System.nanoTime();
		var response = mockMvc.perform(get("/api/v1/owners")
				.with(user("admin").roles("ADMIN")));
		Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", greaterThan(9999)));
		assertThat(elapsed.toMillis(), lessThan(LIMIT.toMillis()));
	}
}
