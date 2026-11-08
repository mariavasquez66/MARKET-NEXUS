package application.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class HexagonalArchitectureTest {
    private static final List<String> FORBIDDEN_DOMAIN_IMPORTS = List.of(
            "org.springframework",
            "jakarta.persistence",
            "jakarta.servlet",
            "application.adapters",
            "application.infrastructure");

    @Test
    void domainDoesNotDependOnFrameworksOrOuterLayers() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/application/domain"))) {
            List<String> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(HexagonalArchitectureTest::violations)
                    .toList();

            assertThat(violations)
                    .as("domain imports must stay framework-free")
                    .isEmpty();
        }
    }

    private static Stream<String> violations(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .filter(line -> FORBIDDEN_DOMAIN_IMPORTS.stream()
                            .anyMatch(forbidden -> line.contains(forbidden)))
                    .map(line -> file + ": " + line);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + file, ex);
        }
    }
}
