package dukono.japp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(Prop.class)
@SpringBootApplication
public class JAppApplication {

	public static void main(final String[] args) {
		SpringApplication.run(JAppApplication.class, args);
	}
}
