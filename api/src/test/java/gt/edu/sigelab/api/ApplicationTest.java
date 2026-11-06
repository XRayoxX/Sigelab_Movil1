package gt.edu.sigelab.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Arranca el contexto real de Spring; la base se sustituye y no se realizan conexiones. */
@SpringBootTest(properties="spring.config.import=")
@AutoConfigureMockMvc
class ApplicationTest {
    @MockitoBean DataSource dataSource;
    @Autowired MockMvc mvc;
    @Test void applicationStartsAndExposesHealth() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void realFilterProtectsDashboard() throws Exception {
        mvc.perform(get("/api/dashboard").servletPath("/api/dashboard"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").exists());
    }
}
