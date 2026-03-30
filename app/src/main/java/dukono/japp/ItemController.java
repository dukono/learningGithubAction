package dukono.japp;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

	private final Prop prop;

	@PostMapping()
	public ResponseEntity<String> getById(@RequestBody final String reques) {
		return ResponseEntity.ok("{ id: %s }".formatted(this.prop.getProp()));
	}
}
