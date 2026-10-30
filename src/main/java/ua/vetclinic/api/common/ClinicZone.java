package ua.vetclinic.api.common;

import java.time.ZoneId;

/** Часовий пояс клініки: у ньому віддаються всі часові мітки відповідей (AD-19.1). */
public final class ClinicZone {

	public static final ZoneId ZONE = ZoneId.of("Europe/Kyiv");

	private ClinicZone() {
	}
}
