package com.mecanica.oficina_api.infrastructure.bdd;

import com.mecanica.oficina_api.infrastructure.integration.PostgresTestcontainersSupport;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Contexto Spring dos cenários BDD: o mesmo dos testes de integração (Postgres no Testcontainers). */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class CucumberSpringConfiguration extends PostgresTestcontainersSupport {
}
