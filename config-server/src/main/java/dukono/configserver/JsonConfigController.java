package dukono.configserver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint para actualizar en caliente la config de una app sin reiniciar el
 * pod.
 *
 * GET /config/json?app=japp devuelve el JSON activo para esa app PUT
 * /config/json?app=japp reemplaza el JSON de esa app en memoria
 *
 * El formato del body es el mismo que Northflank inyecta por variable: {
 * "default": { "application.prop": "valor" }, "stage": { ... } }
 *
 * Ejemplo: curl -u admin:secret -X PUT
 * "https://config-server.xxx/config/json?app=japp" -H "Content-Type:
 * application/json" -d
 * '{"default":{"application.prop":"nuevo"},"stage":{"application.prop":"nuevo-stage"}}'
 */
@RestController
@RequestMapping("/config/json")
public class JsonConfigController {

	private final JsonEnvironmentRepository repository;

	public JsonConfigController(final JsonEnvironmentRepository repository) {
		this.repository = repository;
	}

	/**
	 * Devuelve el JSON actualmente cargado en memoria.
	 */
	@GetMapping(produces = "application/json")
	public ResponseEntity<String> getJson(@RequestParam final String app) {
		final String current = this.repository.getAppJson(app);
		if (current == null || current.isBlank()) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(current);
	}

	/**
	 * Reemplaza el JSON completo en memoria. El cambio es inmediato — la siguiente
	 * llamada a /{app}/{profile} ya usa el nuevo JSON.
	 *
	 * @param json
	 *            nuevo JSON completo con el formato { "appName": { "profile": {
	 *            "key": "value" } } }
	 */
	@PutMapping(consumes = "application/json")
	public ResponseEntity<String> updateJson(@RequestParam final String app, @RequestBody final String json) {
		try {
            this.repository.updateApp(app, json);
			return ResponseEntity.ok("{\"status\":\"updated\",\"app\":\"" + app + "\"}");
		} catch (final IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
		}
	}
}
