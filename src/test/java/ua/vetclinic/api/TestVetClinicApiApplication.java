package ua.vetclinic.api;

import org.springframework.boot.SpringApplication;

public class TestVetClinicApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(VetClinicApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
