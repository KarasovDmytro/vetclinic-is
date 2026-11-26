package ua.vetclinic.api;

import org.springframework.boot.SpringApplication;

/** Локальний запуск застосунку з базою в Testcontainers ({@code ./mvnw spring-boot:test-run}). */
public class TestVetClinicApiApplication {

  /**
   * Запускає застосунок разом із контейнером PostgreSQL.
   *
   * @param args аргументи командного рядка Spring Boot
   */
  public static void main(String[] args) {
    SpringApplication.from(VetClinicApiApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
