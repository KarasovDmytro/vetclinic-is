package ua.vetclinic.api.visits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
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
import static ua.vetclinic.api.TestNotifications.notificationsOf;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.newDoctor;
import static ua.vetclinic.api.TestVisits.postVisit;
import static ua.vetclinic.api.TestVisits.visitBodyWith;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.notifications.LowStockNotifier;

/** REQ-NF-005, AD-07.2: збій на останньому кроці збереження прийому скасовує всі попередні кроки. */
@ApiIntegrationTest
class VisitAtomicityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean
	private LowStockNotifier notifier;

	private Doctor doctor;

	private String patientId;

	private String appointmentId;

	private String medicationId;

	@BeforeEach
	void scheduleAppointment() throws Exception {
		doctor = newDoctor(mockMvc, "Лікар Атомарності");
		patientId = createPatientWithOwner(mockMvc, "Пацієнт Атомарності");
		appointmentId = createAppointment(mockMvc, doctor.id(), patientId, slot("2026-12-02", "10:00"));
		medicationId = createMedication(mockMvc, uniqueName("Препарат атомарності"), 10, 8);
	}

	@Test
	@DisplayName("REQ-NF-005: збій створення сповіщення — 500, прийому немає, запис SCHEDULED, залишок повернуто")
	void failureAfterWithdrawal_rollsEverythingBack() throws Exception {
		// Arrange
		doThrow(new IllegalStateException("збій сповіщення")).when(notifier)
				.notifyLowStock(any(), any(), anyInt(), any());

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, visitBodyWith(medicationId, 5));

		// Assert
		response.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code", equalTo("INTERNAL_ERROR")));
		assertThat(quantityOf(mockMvc, medicationId), equalTo(10));
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")))
				.andExpect(jsonPath("$.visitId", equalTo(null)));
		String history = mockMvc.perform(get("/api/v1/patients/" + patientId + "/visits")
				.header("Authorization", doctor.bearer())).andReturn().getResponse().getContentAsString();
		assertThat((Integer) JsonPath.read(history, "$.total"), equalTo(0));
		assertThat(notificationsOf(mockMvc, medicationId), empty());
	}

	@Test
	@DisplayName("REQ-NF-005: без збою той самий запит завершується успіхом — 201, залишок 5, сповіщення створено")
	void withoutFailure_sameRequestSucceeds() throws Exception {
		// Arrange
		String requestBody = visitBodyWith(medicationId, 5);

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated());
		assertThat(quantityOf(mockMvc, medicationId), equalTo(5));
		assertThat(notificationsOf(mockMvc, medicationId).size(), equalTo(1));
	}
}
