package dukono.configserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * IT que verifica que DynamicGitEnvironmentRepository sirve propiedades desde
 * un repo Git local (file://) creado en un directorio temporal.
 *
 * No necesita red — funciona en cualquier entorno CI/CD.
 *
 * Escenarios cubiertos: 1. Perfil "default" → lee MyApp.yaml 2. Perfil "stage"
 * → lee MyApp-stage.yaml (sobrescribe la prop) 3. App sin GIT_URI → el servidor
 * responde 404 / empty (no revienta)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DynamicGitEnvironmentRepositoryIT {

	// -------------------------------------------------------------------------
	// Repo Git local creado antes de arrancar el contexto Spring
	// -------------------------------------------------------------------------

	private static Path repoDir;

	@BeforeAll
	static void createLocalGitRepo() throws Exception {
		repoDir = Files.createTempDirectory("config-it-repo-");

		// Estructura: config/MyApp.yaml y config/MyApp-stage.yaml
		final Path configDir = repoDir.resolve("config");
		Files.createDirectories(configDir);

		Files.writeString(configDir.resolve("MyApp.yaml"), "application:\n  prop: 'valor-default'\n");

		Files.writeString(configDir.resolve("MyApp-stage.yaml"), "application:\n  prop: 'valor-stage'\n");

		// git init + commit con rama "main"
		run(repoDir, "git", "init", "-b", "main");
		run(repoDir, "git", "config", "user.email", "test@test.com");
		run(repoDir, "git", "config", "user.name", "Test");
		run(repoDir, "git", "add", ".");
		run(repoDir, "git", "commit", "-m", "init");
	}

	@AfterAll
	static void cleanup() throws IOException {
		deleteDir(repoDir);
	}

	// inyecta GIT_URI_MyApp apuntando al repo local ANTES de arrancar el contexto
	@DynamicPropertySource
	static void gitProperties(final DynamicPropertyRegistry registry) {
		registry.add("GIT_URI_MyApp", () -> "file://" + repoDir.toAbsolutePath());
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	@Autowired
	private MockMvc mockMvc;

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void defaultProfile_returnsDefaultProp() throws Exception {

		this.mockMvc.perform(get("/MyApp/default/main")).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("MyApp"))
				.andExpect(jsonPath("$.propertySources[?(@.name =~ /.*MyApp.yaml.*/i)].source['application.prop']")
						.value("valor-default"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void stageProfile_returnsStageProp() throws Exception {
		this.mockMvc.perform(get("/MyApp/stage/main")).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("MyApp"))
				// stage sobreescribe el valor default
				.andExpect(
						jsonPath("$.propertySources[?(@.name =~ /.*MyApp-stage.yaml.*/i)].source['application.prop']")
								.value("valor-stage"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void unknownApp_withoutGitUri_returnsEmptyEnvironment() throws Exception {
		// "UnknownApp" no tiene GIT_URI_UnknownApp →
		// DynamicGitEnvironmentRepository devuelve un Environment vacío (no explota).
		// El Config Server lo serializa como 200 con propertySources vacío.
		this.mockMvc.perform(get("/UnknownApp/default/main")).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("UnknownApp")).andExpect(jsonPath("$.propertySources").isEmpty());
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static void run(final Path dir, final String... cmd) throws Exception {
		final int exit = new ProcessBuilder(List.of(cmd)).directory(dir.toFile()).inheritIO().start().waitFor();
		if (exit != 0)
			throw new RuntimeException("Comando falló: " + String.join(" ", cmd));
	}

	private static void deleteDir(final Path dir) throws IOException {
		if (dir == null || !Files.exists(dir))
			return;
		try (final var stream = Files.walk(dir)) {
			stream.sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);
		}
	}
}
