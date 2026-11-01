package ua.vetclinic.api.visits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.quantityOf;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.newDoctor;
import static ua.vetclinic.api.TestVisits.postVisit;
import static ua.vetclinic.api.TestVisits.visitBodyWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;

/** REQ-F-045, REQ-F-046, REQ-F-048, REQ-NF-005, AD-18.7: списання медикаментів під час збереження прийому. */
@ApiIntegrationTest
class CreateVisitStockTest {

	@Autowired
	private MockMvc mockMvc;

	private Doctor doctor;

	private String patientId;

	private String appointmentId;

	@BeforeEach
	void scheduleAppointment() throws Exception {
		doctor = newDoctor(mockMvc, "Лікар Списання");
		patientId = createPatientWithOwner(mockMvc, "Пацієнт Списання");
		appointmentId = createAppointment(mockMvc, doctor.id(), patientId, slot("2026-11-05", "10:00"));
	}

	private int visitsOfPatient() throws Exception {
		var body = mockMvc.perform(get("/api/v1/patients/" + patientId + "/visits")
				.header("Authorization", doctor.bearer()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.total");
	}

	@Test
	@DisplayName("REQ-F-045: збереження прийому зменшує залишок препарату — 10 − 3 = 7")
	void createVisit_decreasesStock() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Амоксицилін списання"), 10, null);

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, visitBodyWith(medicationId, 3));

		// Assert
		response.andExpect(status().isCreated());
		assertThat(quantityOf(mockMvc, medicationId), equalTo(7));
	}

	@Test
	@DisplayName("REQ-F-045: кілька препаратів списуються кожен на свою кількість")
	void createVisit_decreasesEachMedication() throws Exception {
		// Arrange
		String first = createMedication(mockMvc, uniqueName("Перший препарат"), 10, null);
		String second = createMedication(mockMvc, uniqueName("Другий препарат"), 20, null);
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + first
				+ "\", \"quantity\": 4 }, { \"medicationId\": \"" + second + "\", \"quantity\": 15 } ] }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.medications", hasSize(2)));
		assertThat(quantityOf(mockMvc, first), equalTo(6));
		assertThat(quantityOf(mockMvc, second), equalTo(5));
	}

	@Test
	@DisplayName("REQ-F-048: списання всього залишку допустиме — залишок стає 0, а не від'ємним")
	void withdrawingWholeStock_leavesZero() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Весь залишок"), 5, null);

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, visitBodyWith(medicationId, 5));

		// Assert
		response.andExpect(status().isCreated());
		assertThat(quantityOf(mockMvc, medicationId), equalTo(0));
	}

	@Test
	@DisplayName("REQ-F-046: запитано більше за залишок — 409 INSUFFICIENT_STOCK з дослівним текстом із BDD")
	void insufficientStock_returns409WithBddMessage() throws Exception {
		// Arrange
		String name = uniqueName("Новокаїн");
		String medicationId = createMedication(mockMvc, name, 5, null);

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, visitBodyWith(medicationId, 8));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("INSUFFICIENT_STOCK")))
				.andExpect(jsonPath("$.message",
						equalTo("Недостатній залишок: " + name + " (доступно: 5, запитано: 8)")));
	}

	@Test
	@DisplayName("AD-18.7: details містить позицію запиту, причину INSUFFICIENT_STOCK і params medicationId, name, available, requested")
	void insufficientStock_describesProblemMedication() throws Exception {
		// Arrange
		String enough = createMedication(mockMvc, uniqueName("Достатньо"), 10, null);
		String name = uniqueName("Бракує");
		String lacking = createMedication(mockMvc, name, 5, null);
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + enough
				+ "\", \"quantity\": 2 }, { \"medicationId\": \"" + lacking + "\", \"quantity\": 8 } ] }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.details", hasSize(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("medications[1].quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INSUFFICIENT_STOCK")))
				.andExpect(jsonPath("$.details[0].params.medicationId", equalTo(lacking)))
				.andExpect(jsonPath("$.details[0].params.name", equalTo(name)))
				.andExpect(jsonPath("$.details[0].params.available", equalTo(5)))
				.andExpect(jsonPath("$.details[0].params.requested", equalTo(8)));
	}

	@Test
	@DisplayName("AD-18.7: нестача кількох препаратів — details містить кожен проблемний препарат")
	void insufficientStock_listsEveryLackingMedication() throws Exception {
		// Arrange
		String first = createMedication(mockMvc, uniqueName("Бракує перший"), 1, null);
		String second = createMedication(mockMvc, uniqueName("Бракує другий"), 2, null);
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + first
				+ "\", \"quantity\": 3 }, { \"medicationId\": \"" + second + "\", \"quantity\": 4 } ] }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.details", hasSize(2)))
				.andExpect(jsonPath("$.details[0].field", equalTo("medications[0].quantity")))
				.andExpect(jsonPath("$.details[1].field", equalTo("medications[1].quantity")));
	}

	@Test
	@DisplayName("REQ-F-046, REQ-NF-005: при нестачі не змінюється нічого — немає прийому, запис SCHEDULED, залишки всіх препаратів на місці")
	void insufficientStock_changesNothing() throws Exception {
		// Arrange
		String enough = createMedication(mockMvc, uniqueName("Достатньо без змін"), 10, null);
		String lacking = createMedication(mockMvc, uniqueName("Бракує без змін"), 1, null);
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + enough
				+ "\", \"quantity\": 4 }, { \"medicationId\": \"" + lacking + "\", \"quantity\": 2 } ] }";

		// Act
		postVisit(mockMvc, doctor, appointmentId, requestBody).andExpect(status().isConflict());

		// Assert
		assertThat(quantityOf(mockMvc, enough), equalTo(10));
		assertThat(quantityOf(mockMvc, lacking), equalTo(1));
		assertThat(visitsOfPatient(), equalTo(0));
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")))
				.andExpect(jsonPath("$.visitId", equalTo(null)));
	}

	@Test
	@DisplayName("REQ-F-046: після відмови через нестачу той самий запис можна зберегти з меншою кількістю — 201")
	void afterInsufficientStock_visitCanBeSavedWithSmallerQuantity() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Повторна спроба"), 5, null);
		postVisit(mockMvc, doctor, appointmentId, visitBodyWith(medicationId, 8)).andExpect(status().isConflict());

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, visitBodyWith(medicationId, 5));

		// Assert
		response.andExpect(status().isCreated());
		assertThat(quantityOf(mockMvc, medicationId), equalTo(0));
	}

	@Test
	@DisplayName("REQ-F-048: препарат із нульовим залишком не можна списати — 409 INSUFFICIENT_STOCK, доступно 0")
	void emptyStock_returns409() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Порожній"), 0, null);

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, visitBodyWith(medicationId, 1));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.details[0].params.available", equalTo(0)))
				.andExpect(jsonPath("$.details[0].params.requested", equalTo(1)));
	}

	@Test
	@DisplayName("REQ-F-048, REQ-NF-005: 8 паралельних прийомів по 1 одиниці при залишку 5 — рівно 5 успіхів, 3 відмови, залишок 0")
	void concurrentVisits_neverOverdrawStock() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Паралельне списання"), 5, null);
		List<String> appointments = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			String time = String.format("%02d:00", 9 + i);
			appointments.add(createAppointment(mockMvc, doctor.id(),
					createPatientWithOwner(mockMvc, "Паралельний Пацієнт " + i), slot("2026-11-06", time)));
		}
		ExecutorService executor = Executors.newFixedThreadPool(8);
		List<Callable<Integer>> calls = new ArrayList<>();
		for (String appointment : appointments) {
			calls.add(() -> postVisit(mockMvc, doctor, appointment, visitBodyWith(medicationId, 1)).andReturn()
					.getResponse().getStatus());
		}

		// Act
		List<Future<Integer>> results = executor.invokeAll(calls);
		executor.shutdown();

		// Assert
		long created = 0;
		long conflicts = 0;
		for (Future<Integer> result : results) {
			created += result.get() == 201 ? 1 : 0;
			conflicts += result.get() == 409 ? 1 : 0;
		}
		assertThat(created, equalTo(5L));
		assertThat(conflicts, equalTo(3L));
		assertThat(quantityOf(mockMvc, medicationId), equalTo(0));
	}
}
