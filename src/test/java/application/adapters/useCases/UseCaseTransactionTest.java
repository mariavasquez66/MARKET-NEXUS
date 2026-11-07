package application.adapters.useCases;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class UseCaseTransactionTest {

    @Test
    void allUseCasesHaveTransactionBoundary() {
        List<Class<?>> useCases = List.of(
                AccountUseCase.class,
                BusinessCustomerUseCase.class,
                BusinessOperatorUseCase.class,
                BusinessSupervisorUseCase.class,
                NaturalCustomerUseCase.class,
                PublicAccessUseCase.class,
                TellerEmployeeUseCase.class);

        assertThat(useCases).allMatch(type -> type.isAnnotationPresent(Transactional.class));
    }
}
