package ua.vetclinic.api.users;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ua.vetclinic.api.ApiIntegrationTest;

/** AD-17.2: унікальність email без урахування регістру гарантує сама БД, а не лише перевірка в сервісі. */
@ApiIntegrationTest
@Transactional
class UserEmailUniquenessTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private void insertUser(String email) {
		jdbcTemplate.update("""
				INSERT INTO users (id, email, full_name, role, active, created_at, password_hash)
				VALUES (?::uuid, ?, 'Дублікат Email', 'VETERINARIAN', TRUE, now(), 'hash')
				""", UUID.randomUUID().toString(), email);
	}

	@Test
	@DisplayName("AD-17.2: другий запис з тим самим email — БД відхиляє")
	void sameEmail_isRejectedByDatabase() {
		// Arrange
		insertUser("db.unique@clinic.example");

		// Act
		Runnable insertDuplicate = () -> insertUser("db.unique@clinic.example");

		// Assert
		assertThrows(DataIntegrityViolationException.class, insertDuplicate::run);
	}

	@Test
	@DisplayName("AD-17.2: email, що відрізняється лише регістром, — БД відхиляє")
	void emailInOtherCase_isRejectedByDatabase() {
		// Arrange
		insertUser("db.case@clinic.example");

		// Act
		Runnable insertOtherCase = () -> insertUser("DB.Case@Clinic.Example");

		// Assert
		assertThrows(DataIntegrityViolationException.class, insertOtherCase::run);
	}
}
