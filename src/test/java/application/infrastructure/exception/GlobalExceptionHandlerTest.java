package application.infrastructure.exception;

import application.domain.exceptions.common.InsufficientPermissionException;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.common.UnauthenticatedUserException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.exceptions.order.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mapsBadRequestFamily() throws Exception {
        mockMvc.perform(get("/test/400").requestAttr("requestId", "req-400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_DATA"))
                .andExpect(jsonPath("$.message").value("bad input"))
                .andExpect(jsonPath("$.path").value("/test/400"))
                .andExpect(jsonPath("$.requestId").value("req-400"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void mapsUnauthenticatedFamily() throws Exception {
        mockMvc.perform(get("/test/401").requestAttr("requestId", "req-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value("/test/401"))
                .andExpect(jsonPath("$.requestId").value("req-401"));
    }

    @Test
    void mapsForbiddenFamily() throws Exception {
        mockMvc.perform(get("/test/403").requestAttr("requestId", "req-403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/test/403"))
                .andExpect(jsonPath("$.requestId").value("req-403"));
    }

    @Test
    void mapsNotFoundFamily() throws Exception {
        mockMvc.perform(get("/test/404").requestAttr("requestId", "req-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/test/404"))
                .andExpect(jsonPath("$.requestId").value("req-404"));
    }

    @Test
    void mapsConflictFamily() throws Exception {
        mockMvc.perform(get("/test/409").requestAttr("requestId", "req-409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.path").value("/test/409"))
                .andExpect(jsonPath("$.requestId").value("req-409"));
    }

    @Test
    void mapsDataIntegrityViolationToConflict() throws Exception {
        mockMvc.perform(get("/test/integrity").requestAttr("requestId", "req-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("Resource conflicts with existing data"))
                .andExpect(jsonPath("$.path").value("/test/integrity"))
                .andExpect(jsonPath("$.requestId").value("req-integrity"));
    }

    @Test
    void mapsUnexpectedErrorWithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/500").requestAttr("requestId", "req-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/test/500"))
                .andExpect(jsonPath("$.requestId").value("req-500"))
                .andExpect(content().string(not(containsString("sensitive internal detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at application."))));
    }

    @Test
    void generatesRequestIdWhenAttributeIsMissing() throws Exception {
        mockMvc.perform(get("/test/400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/400")
        String badRequest() {
            throw new InvalidDataException("bad input");
        }

        @GetMapping("/test/401")
        String unauthenticated() {
            throw new UnauthenticatedUserException("missing token");
        }

        @GetMapping("/test/403")
        String forbidden() {
            throw new InsufficientPermissionException("denied");
        }

        @GetMapping("/test/404")
        String notFound() {
            throw new OrderNotFoundException("order missing");
        }

        @GetMapping("/test/409")
        String conflict() {
            throw new InvalidStateTransitionException("cannot transition");
        }

        @GetMapping("/test/integrity")
        String integrity() {
            throw new DataIntegrityViolationException("duplicate key");
        }

        @GetMapping("/test/500")
        String unexpected() {
            throw new IllegalStateException("sensitive internal detail");
        }
    }
}
