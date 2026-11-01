package application.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static application.domain.DomainFixtures.user;

import application.domain.enums.UserRole;
import application.domain.enums.UserStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.user.InactiveUserException;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void newUserIsActiveWithZeroTokenVersion() {
        User user = user(UserRole.BUYER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getAuthTokenVersion()).isZero();
        assertThat(user.getId()).isNotBlank();
    }

    @Test
    void blockInvalidatesTokensAndPreventsOperation() {
        User user = user(UserRole.BUYER);
        user.block();
        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(user.getAuthTokenVersion()).isEqualTo(1);
        assertThatThrownBy(user::assertCanOperate).isInstanceOf(InactiveUserException.class);
    }

    @Test
    void invalidTransitionsFail() {
        User user = user(UserRole.BUYER);
        assertThatThrownBy(user::activate).isInstanceOf(InvalidDataException.class);
        user.deactivate();
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        user.activate();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void roleCannotChange() {
        User user = user(UserRole.SELLER);
        assertThat(user.hasRole(UserRole.SELLER)).isTrue();
        assertThat(user.hasRole(UserRole.ADMIN)).isFalse();
    }
}
