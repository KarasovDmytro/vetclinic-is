package ua.vetclinic.api;

/** Спільні шаблони та значення для перевірок у інтеграційних тестах. */
public final class TestData {

	public static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	public static final String CLINIC_OFFSET_TIMESTAMP =
			"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\+0[23]:00";

	public static final String LONG_EMAIL = "a".repeat(64) + "@" + ("b".repeat(60) + ".").repeat(3) + "example";

	private TestData() {
	}
}
