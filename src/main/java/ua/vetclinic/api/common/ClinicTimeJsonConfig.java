package ua.vetclinic.api.common;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
class ClinicTimeJsonConfig {

	@Bean
	JacksonModule clinicZoneModule() {
		SimpleModule module = new SimpleModule("clinic-zone");
		module.addSerializer(OffsetDateTime.class, new ValueSerializer<OffsetDateTime>() {
			@Override
			public void serialize(OffsetDateTime value, JsonGenerator generator, SerializationContext context) {
				generator.writeString(
						DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.atZoneSameInstant(ClinicZone.ZONE)));
			}
		});
		return module;
	}
}
