package dukono.configserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentProperties;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

/**
 * EnvironmentRepository dinámico.
 *
 * Lee variables de entorno con el patrón: GIT_URI_<AppName> (obligatorio para
 * registrar la app) GIT_TOKEN_<AppName> (opcional, para repos privados)
 * GIT_USERNAME_<AppName> (opcional) GIT_BRANCH_<AppName> (opcional, default
 * "main")
 *
 * Ejemplo para registrar "App" y "App2": GIT_URI_App =
 * https://github.com/user/my-app GIT_TOKEN_App = ghp_xxxx GIT_URI_App2 =
 * https://github.com/user/my-app2 GIT_TOKEN_App2 = ghp_yyyy
 *
 * Estructura esperada en CADA repo: config/ App.yaml <- perfil default
 * App-stage.yaml <- perfil stage
 */
@Component
public class DynamicGitEnvironmentRepository implements EnvironmentRepository {

	private static final Logger log = LoggerFactory.getLogger(DynamicGitEnvironmentRepository.class);
	private static final String URI_PREFIX = "GIT_URI_";

	private final Map<String, MultipleJGitEnvironmentRepository> repoCache = new ConcurrentHashMap<>();

	private final ConfigurableEnvironment springEnv;
	private final ObservationRegistry observationRegistry;

	public DynamicGitEnvironmentRepository(final ConfigurableEnvironment springEnv,
			final ObservationRegistry observationRegistry) {
		this.springEnv = springEnv;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public Environment findOne(final String application, final String profile, final String label) {
		final String uri = this.springEnv.getProperty(URI_PREFIX + application);
		if (uri == null || uri.isBlank()) {
			// No hay repo registrado para esta app (ej: el health-check interno llama
			// con application="app"). Devolvemos Environment vacío en lugar de explotar.
			log.error("No existe {} → devolviendo Environment vacío para '{}'", URI_PREFIX + application, application);
			return new Environment(application, profile);
		}
		final MultipleJGitEnvironmentRepository repo = this.repoCache.computeIfAbsent(application, this::buildRepo);
		return repo.findOne(application, profile, label);
	}

	// -------------------------------------------------------------------------

	private MultipleJGitEnvironmentRepository buildRepo(final String appName) {
		final String uri = this.springEnv.getProperty(URI_PREFIX + appName);
		if (uri == null || uri.isBlank()) {
			throw new IllegalStateException("No hay repo configurado para la aplicación '" + appName + "'. "
					+ "Define la variable de entorno: " + URI_PREFIX + appName);
		}

		final String token = this.springEnv.getProperty("GIT_TOKEN_" + appName, "");
		final String username = this.springEnv.getProperty("GIT_USERNAME_" + appName, "");
		final String branch = this.springEnv.getProperty("GIT_BRANCH_" + appName, "main");
		final String path = this.springEnv.getProperty("GIT_PATH_" + appName, "config");

		log.info("Construyendo repo Git para '{}': uri={}, branch={}", appName, uri, branch);

		final MultipleJGitEnvironmentProperties props = new MultipleJGitEnvironmentProperties();
		props.setUri(uri);
		props.setUsername(username);
		props.setPassword(token);
		props.setDefaultLabel(branch);
		props.setSearchPaths(path);
		props.setForcePull(true);
		props.setCloneOnStart(false);

		final MultipleJGitEnvironmentRepository repo = new MultipleJGitEnvironmentRepository(this.springEnv, props,
				this.observationRegistry);
		try {
			repo.afterPropertiesSet();
		} catch (final Exception e) {
			throw new RuntimeException("Error inicializando repo Git para '" + appName + "'", e);
		}
		return repo;
	}
}
