package dev.incidentcopilot.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantController.class)
class TenantControllerTest {
    @Autowired MockMvc mvc;
    @Test void returnsKnownTenant() throws Exception { mvc.perform(get("/tenants/tenant-217")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE")); }
    @Test void rejectsUnknownTenant() throws Exception { mvc.perform(get("/tenants/tenant-999")).andExpect(status().isNotFound()); }
}
