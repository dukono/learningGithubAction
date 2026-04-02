package dukono.configserver;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
/**
 * IT que verifica que JsonEnvironmentRepository sirve properties
 * desde variables de entorno individuales por app (formato Northflank).
 *
 * Northflank inyecta cada clave del secret-group como variable independiente:
 *   japp    = { "default":{...}, "stage":{...} }
 *   support = { "default":{...} }
 *
 * En tests usamos updateApp() porque System.getenv() no es modificable en JVM.
 * Es el mismo mecanismo que PUT /config/json?app= usa internamente.
 *
 * Escenarios:
 *  1. Perfil "default"     -> devuelve prop del bloque "default"
 *  2. Perfil "stage"       -> devuelve fuente stage + fuente default
 *  3. Perfil no definido   -> solo fuente "default"
 *  4. App sin variable     -> 200 con propertySources vacío
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonEnvironmentRepositoryIT {
    @Autowired
    private JsonEnvironmentRepository repository;
    @Autowired
    private MockMvc mockMvc;
    @BeforeAll
    void setup() {
        repository.updateApp("japp",
                "{\"default\":{\"application.prop\":\"valor-default\"},\"stage\":{\"application.prop\":\"valor-stage\"}}");
        repository.updateApp("support",
                "{\"default\":{\"application.prop2\":\"valor-support-default\"}}");
    }
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void defaultProfile_returnsDefaultProp() throws Exception {
        this.mockMvc.perform(get("/japp/default/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("japp"))
                .andExpect(jsonPath(
                        "$.propertySources[?(@.name == 'json-config/japp/default')].source['application.prop']")
                        .value("valor-default"));
    }
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void stageProfile_returnsStageAndDefaultSources() throws Exception {
        this.mockMvc.perform(get("/japp/stage/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("japp"))
                .andExpect(jsonPath(
                        "$.propertySources[?(@.name == 'json-config/japp/stage')].source['application.prop']")
                        .value("valor-stage"))
                .andExpect(jsonPath(
                        "$.propertySources[?(@.name == 'json-config/japp/default')].source['application.prop']")
                        .value("valor-default"));
    }
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unknownProfile_returnsOnlyDefaultSource() throws Exception {
        this.mockMvc.perform(get("/japp/prod/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("japp"))
                .andExpect(jsonPath(
                        "$.propertySources[?(@.name == 'json-config/japp/default')].source['application.prop']")
                        .value("valor-default"))
                .andExpect(jsonPath("$.propertySources[?(@.name == 'json-config/japp/prod')]").doesNotExist());
    }
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void appWithoutEnvVar_returnsEmptyPropertySources() throws Exception {
        this.mockMvc.perform(get("/ghost/default/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ghost"))
                .andExpect(jsonPath("$.propertySources").isEmpty());
    }
}
