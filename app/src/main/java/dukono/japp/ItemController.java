package dukono.japp;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    
    @PostMapping()
    public ResponseEntity<String> getById(@RequestBody String reques) {
        return ResponseEntity.ok("{ id: Hola }");
    }
}
