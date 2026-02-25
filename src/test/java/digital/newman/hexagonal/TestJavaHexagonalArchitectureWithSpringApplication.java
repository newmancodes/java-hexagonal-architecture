package digital.newman.hexagonal;

import org.springframework.boot.SpringApplication;

public class TestJavaHexagonalArchitectureWithSpringApplication {

	public static void main(String[] args) {
		SpringApplication.from(JavaHexagonalArchitectureWithSpringApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
