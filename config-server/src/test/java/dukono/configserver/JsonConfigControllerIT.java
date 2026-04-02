package dukono.configserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * IT que verifica el flujo completo de PUT/GET /config/json?app=
 *
 * 1. GET ?app=japp sin override → 204 (no hay variable de entorno "japp" en
 * este contexto) 2. PUT ?app=japp con JSON válido → 200
 * {"status":"updated","app":"japp"} 3. GET ?app=japp tras PUT → 200 con el JSON
 * recién guardado 4. GET /japp/stage/main → props del override en memoria 5.
 * PUT con JSON inválido → 400 6. PUT sin autenticación → 401
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JsonConfigControllerIT {

	private static final String VALID_JSON = "{\"default\":{\"application.prop\":\"valor-default-controller\"},"
			+ "\"stage\":{\"application.prop\":\"valor-stage-controller\"}}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@Order(1)
	@WithMockUser(username = "admin", roles = "ADMIN")
	void getJson_whenNoOverrideAndNoEnvVar_returns204() throws Exception {
		this.mockMvc.perform(get("/config/json").param("app", "japp")).andExpect(status().isNoContent());
	}

	@Test
	@Order(2)
	@WithMockUser(username = "admin", roles = "ADMIN")
	void putJson_withValidJson_returns200() throws Exception {
		this.mockMvc
				.perform(put("/config/json").param("app", "japp").contentType(MediaType.APPLICATION_JSON)
						.content(VALID_JSON))
				.andExpect(status().isOk()).andExpect(content().json("{\"status\":\"updated\",\"app\":\"japp\"}"));
	}

	@Test
	@Order(3)
	@WithMockUser(username = "admin", roles = "ADMIN")
	void getJson_afterPut_returnsStoredJson() throws Exception {
		this.mockMvc.perform(get("/config/json").param("app", "japp")).andExpect(status().isOk())
				.andExpect(jsonPath("$.stage['application.prop']").value("valor-stage-controller"));
	}

	@Test
	@Order(4)
	@WithMockUser(username = "admin", roles = "ADMIN")
	void configEndpoint_afterPut_servesPropertiesFromOverride() throws Exception {
		this.mockMvc.perform(get("/japp/stage/main")).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("japp")).andExpect(
						jsonPath("$.propertySources[?(@.name == 'json-config/japp/stage')].source['application.prop']")
								.value("valor-stage-controller"));
	}

	@Test
	@Order(5)
	@WithMockUser(username = "admin", roles = "ADMIN")
	void putJson_withInvalidJson_returns400() throws Exception {
		this.mockMvc.perform(put("/config/json").param("app", "japp").contentType(MediaType.APPLICATION_JSON)
				.content("{ esto no es json }")).andExpect(status().isBadRequest());
	}

	@Test
	@Order(6)
	void putJson_withoutAuth_returns401() throws Exception {
		this.mockMvc.perform(
				put("/config/json").param("app", "japp").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
				.andExpect(status().isUnauthorized());
	}
}
