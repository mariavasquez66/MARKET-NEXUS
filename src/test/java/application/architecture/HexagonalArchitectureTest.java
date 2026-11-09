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
            "com.fasterxml.jackson",
            "tools.jackson",
            "application.adapters",
            "application.infrastructure");

    private static final List<String> ALLOWED_CONTROLLER_IMPORTS = List.of(
            "application.adapters.rest.dto.",
            "application.adapters.rest.mappers.",
            "application.domain.models.",
            "application.domain.enums.",
            "application.domain.valueobjects.",
            "application.domain.exceptions.",
            "application.domain.ports.in.");

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

    @Test
    void controllersDependOnInputPortsInsteadOfDomainServicesOrAdapters() throws IOException {
        try (Stream<Path> files = Files.walk(
                Path.of("src/main/java/application/adapters/rest/controllers"))) {
            List<String> violations = files
                    .filter(path -> path.toString().endsWith("Controller.java"))
                    .flatMap(HexagonalArchitectureTest::controllerViolations)
                    .toList();

            assertThat(violations)
                    .as("controllers may only depend on input ports, REST DTO/mappers and domain types")
                    .isEmpty();
        }
    }

    private static Stream<String> controllerViolations(Path file) {
        try {
            List<String> imports = Files.readAllLines(file).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import application."))
                    .toList();
            Stream<String> forbidden = imports.stream()
                    .filter(line -> ALLOWED_CONTROLLER_IMPORTS.stream()
                            .noneMatch(allowed -> line.contains(allowed)))
                    .map(line -> file + ": " + line);
            if (imports.stream().noneMatch(line -> line.contains("application.domain.ports.in."))) {
                return Stream.concat(forbidden,
                        Stream.of(file + ": controller does not depend on an input port"));
            }
            return forbidden;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + file, ex);
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
