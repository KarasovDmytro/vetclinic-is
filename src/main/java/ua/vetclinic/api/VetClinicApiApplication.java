package ua.vetclinic.api;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ua.vetclinic.api.common.ClinicZone;

@SpringBootApplication
public class VetClinicApiApplication {

	public static void main(String[] args) {
		// Драйвер PostgreSQL передає серверу часовий пояс JVM; застаріла назва Europe/Kiev з Windows сервер відхиляє.
		TimeZone.setDefault(TimeZone.getTimeZone(ClinicZone.ZONE));
		SpringApplication.run(VetClinicApiApplication.class, args);
	}

}
