package dukono.configserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * EnvironmentRepository que lee properties desde variables de entorno con una
 * variable por aplicación. Northflank inyecta cada clave del secret-group como
 * variable de entorno independiente:
 *
 * <pre>
 * japp  = { "default": { "application.prop": "valor" }, "stage": { ... } }
 * japp2 = { "default": { "application.prop": "valor" }, "stage": { ... } }
 * </pre>
 *
 * Resolución de perfil: 1. Se carga "default" como base (si existe). 2. Se
 * carga el perfil específico encima (sobreescribe default). 3. Si la variable
 * no existe para esa app → Environment vacío,
 * {@link DynamicGitEnvironmentRepository} toma el relevo.
 *
 * Actualización en caliente: PUT /config/json?app=japp (sin reiniciar el pod).
 * El override en memoria tiene prioridad sobre la variable de entorno.
 */
@Component
@Order(1)
public class JsonEnvironmentRepository implements EnvironmentRepository {

	private static final Logger log = LoggerFactory.getLogger(JsonEnvironmentRepository.class);
	private static final TypeReference<Map<String, Map<String, Object>>> PER_APP_TYPE = new TypeReference<>() {
	};

	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Override en caliente por app: app → JSON de perfiles. Si hay entrada aquí
	 * tiene prioridad sobre la variable de entorno.
	 */
	private final ConcurrentHashMap<String, String> liveOverrides = new ConcurrentHashMap<>();

	public JsonEnvironmentRepository() {
	}

	// -------------------------------------------------------------------------
	// API pública para JsonConfigController
	// -------------------------------------------------------------------------

	/**
	 * Actualiza en caliente el JSON de una app concreta sin reiniciar el pod.
	 *
	 * @param appName
	 *            nombre de la app (ej: "japp")
	 * @param json
	 *            JSON de perfiles: { "default":{...}, "stage":{...} }
	 */
	public void updateApp(final String appName, final String json) {
		try {
			this.mapper.readValue(json, PER_APP_TYPE); // valida
		} catch (final Exception e) {
			throw new IllegalArgumentException("JSON inválido para '" + appName + "': " + e.getMessage(), e);
		}
		this.liveOverrides.put(appName, json);
		log.info("Config actualizada en caliente para app='{}' ({} chars)", appName, json.length());
	}

	/**
	 * Devuelve el JSON activo para una app. Prioridad: 1. Override en memoria (via
	 * PUT /config/json?app=) — actualización en caliente 2. System.getenv(appName)
	 * — lo que Northflank inyecta en el secret-group
	 *
	 * Se usa System.getenv() directamente (no springEnv) porque las variables de
	 * entorno del proceso son inmutables durante su vida: al reiniciar el pod,
	 * Northflank inyecta los valores actualizados del secret-group y
	 * System.getenv() los refleja correctamente sin ningún caché de Spring.
	 */
	public String getAppJson(final String appName) {
		final String override = this.liveOverrides.get(appName);
		if (override != null)
			return override;
		return System.getenv(appName);
	}

	// -------------------------------------------------------------------------
	// EnvironmentRepository
	// -------------------------------------------------------------------------

	@Override
	public Environment findOne(final String application, final String profile, final String label) {
		final String json = this.getAppJson(application);

		if (json == null || json.isBlank()) {
			log.debug("No hay variable de entorno '{}' — delegando a Git", application);
			return this.emptyEnv(application, profile);
		}

		final Map<String, Map<String, Object>> appConfig;
		try {
			appConfig = this.mapper.readValue(json, PER_APP_TYPE);
		} catch (final Exception e) {
			log.error("Error al parsear variable '{}': {}", application, e.getMessage());
			return this.emptyEnv(application, profile);
		}

		final Environment env = new Environment(application, profile);

		// 1. "default" como base
		final Map<String, Object> defaultProps = appConfig.get("default");
		if (defaultProps != null) {
			env.add(new PropertySource("json-config/" + application + "/default", this.flatten(defaultProps)));
		}

		// 2. Perfil específico encima
		if (!"default".equals(profile)) {
			final Map<String, Object> profileProps = appConfig.get(profile);
			if (profileProps != null) {
				env.add(new PropertySource("json-config/" + application + "/" + profile, this.flatten(profileProps)));
			} else {
				log.debug("Perfil '{}' no definido en '{}', usando solo 'default'", profile, application);
			}
		}

		log.info("Sirviendo config desde variable de entorno '{}' profile='{}'", application, profile);
		return env;
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private Map<String, Object> flatten(final Map<String, Object> source) {
		final Map<String, Object> result = new HashMap<>();
		this.flatten("", source, result);
		return result;
	}

	@SuppressWarnings("unchecked")
	private void flatten(final String prefix, final Map<String, Object> source, final Map<String, Object> result) {
		for (final Map.Entry<String, Object> entry : source.entrySet()) {
			final String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
			if (entry.getValue() instanceof Map) {
				this.flatten(key, (Map<String, Object>) entry.getValue(), result);
			} else {
				result.put(key, entry.getValue());
			}
		}
	}

	private Environment emptyEnv(final String application, final String profile) {
		return new Environment(application, profile);
	}
}
