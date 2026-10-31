package ua.vetclinic.api.common;

/** Нормалізація текстових полів запиту (AD-17.1: пробіли по краях обрізаються). */
public final class Strings {

	private Strings() {
	}

	public static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
