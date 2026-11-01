package ua.vetclinic.api.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;

import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-NF-006, AD-21: журнал пишеться в консоль і у файли, файли ротуються щодня на 30 днів. */
@ApiIntegrationTest
class LogOutputTest {

	@Autowired
	private MockMvc mockMvc;

	@Value("${vetclinic.logging.dir}")
	private String logDir;

	@Test
	@DisplayName("AD-21.1: подія аудиту потрапляє у файл audit.log каталогу з конфігурації")
	void auditEvent_isWrittenToAuditFile() throws Exception {
		// Arrange
		String userId = createUser(mockMvc, "logout.audit@clinic.example", "Файлович Аудит Петрович", "VETERINARIAN",
				"Vet-Passw0rd-2026");

		// Act
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "logout.audit@clinic.example", "password": "Vet-Passw0rd-2026" }
						"""))
				.andExpect(status().isOk());

		// Assert
		String content = Files.readString(Path.of(logDir, "audit.log"));
		assertThat(content, allOf(containsString("event=LOGIN_SUCCESS"), containsString("actorId=" + userId)));
	}

	@Test
	@DisplayName("AD-21.1, AD-21.5: рядок про HTTP-запит потрапляє у технічний файл application.log")
	void httpRequestLine_isWrittenToApplicationFile() throws Exception {
		// Arrange
		var request = get("/api/v1/users").with(user("admin").roles("ADMIN"));

		// Act
		var result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		// Assert
		String requestId = result.getResponse().getHeader("X-Request-Id");
		String content = Files.readString(Path.of(logDir, "application.log"));
		assertThat(content, containsString("requestId=" + requestId));
	}

	@Test
	@DisplayName("AD-21.3: файл аудиту ротується щодня і зберігається 30 днів")
	void auditFile_rollsDailyAndKeeps30Days() {
		// Arrange
		Logger audit = (Logger) LoggerFactory.getLogger("AUDIT");

		// Act
		var appender = audit.getAppender("AUDIT_FILE");

		// Assert
		assertThat(appender, notNullValue());
		var policy = ((RollingFileAppender<?>) appender).getRollingPolicy();
		assertThat(policy, instanceOf(TimeBasedRollingPolicy.class));
		assertThat(((TimeBasedRollingPolicy<?>) policy).getMaxHistory(), equalTo(30));
		assertThat(((TimeBasedRollingPolicy<?>) policy).getFileNamePattern(), containsString("%d{yyyy-MM-dd}"));
	}

	@Test
	@DisplayName("AD-21.3: технічний файл ротується щодня і зберігається 30 днів")
	void applicationFile_rollsDailyAndKeeps30Days() {
		// Arrange
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

		// Act
		var appender = root.getAppender("APP_FILE");

		// Assert
		assertThat(appender, notNullValue());
		var policy = ((RollingFileAppender<?>) appender).getRollingPolicy();
		assertThat(policy, instanceOf(TimeBasedRollingPolicy.class));
		assertThat(((TimeBasedRollingPolicy<?>) policy).getMaxHistory(), equalTo(30));
		assertThat(((TimeBasedRollingPolicy<?>) policy).getFileNamePattern(), containsString("%d{yyyy-MM-dd}"));
	}

	@Test
	@DisplayName("AD-21.1: журнал аудиту пишеться і в консоль")
	void auditLogger_hasConsoleAppender() {
		// Arrange
		Logger audit = (Logger) LoggerFactory.getLogger("AUDIT");

		// Act
		var appender = audit.getAppender("CONSOLE");

		// Assert
		assertThat(appender, notNullValue());
	}

	@Test
	@DisplayName("AD-21.1: технічний журнал пишеться і в консоль")
	void rootLogger_hasConsoleAppender() {
		// Arrange
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

		// Act
		var appender = root.getAppender("CONSOLE");

		// Assert
		assertThat(appender, notNullValue());
	}

	@Test
	@DisplayName("AD-21.4: збій запису в журнал аудиту не перериває бізнес-операцію")
	void failingAuditAppender_doesNotBreakLogin() throws Exception {
		// Arrange
		createUser(mockMvc, "logout.failing@clinic.example", "Збійний Запис Петрович", "VETERINARIAN",
				"Vet-Passw0rd-2026");
		Logger audit = (Logger) LoggerFactory.getLogger("AUDIT");
		AppenderBase<ILoggingEvent> failing = new AppenderBase<>() {
			@Override
			protected void append(ILoggingEvent event) {
				throw new IllegalStateException("disk is full");
			}
		};
		failing.setContext(audit.getLoggerContext());
		failing.start();
		audit.addAppender(failing);

		try {
			// Act
			var response = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "email": "logout.failing@clinic.example", "password": "Vet-Passw0rd-2026" }
							"""));

			// Assert
			response.andExpect(status().isOk());
		}
		finally {
			audit.detachAppender(failing);
			failing.stop();
		}
	}
}
