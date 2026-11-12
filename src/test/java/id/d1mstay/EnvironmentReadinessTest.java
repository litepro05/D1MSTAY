package id.d1mstay;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EnvironmentReadinessTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entities;
    @Autowired SpringTemplateEngine templates;
    @Autowired MockMvc mvc;

    @Test
    void javaSpringHibernateThymeleafAndPostgresWorkTogether() throws Exception {
        assertThat(Runtime.version().feature()).isGreaterThanOrEqualTo(21);
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("d1mstay_praktikum");
        assertThat(jdbc.queryForObject("select current_user", String.class)).isEqualTo("d1mstay_app");
        assertThat(entities.isOpen()).isTrue();
        assertThat(templates).isNotNull();
        mvc.perform(get("/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.application").value("D1MSTAY"))
            .andExpect(jsonPath("$.status").value("ready"));
    }
}
