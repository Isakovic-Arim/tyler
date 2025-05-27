package tyler.server.integration.resource.user;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import tyler.server.integration.resource.BaseResourceTest;
import tyler.server.repository.RefreshTokenRepository;
import tyler.server.repository.UserRepository;
import tyler.server.entity.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Map;

class UserResourceTest extends BaseResourceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User user;
    private Map<String, String> cookies;

    @BeforeAll
    void setup() {
        user = User.builder()
                .username("user")
                .passwordHash(passwordEncoder.encode("test"))
                .currentXp(0)
                .dailyXpQuota(10)
                .currentStreak(0)
                .daysOffPerWeek((byte) 2)
                .build();
        userRepository.save(user);

        cookies = getAuthCookies(user.getUsername(), "test");
    }

    @AfterEach
    void cleanUpEach() {
        user.setDaysOff(new HashSet<>());
        userRepository.save(user);
    }

    @AfterAll
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.delete(user);
    }

    @Test
    @WithMockUser(username = "user")
    void setDayOff_hasEnoughDaysOff_returnsNoContent() {
        user.setDaysOffPerWeek((byte) 2);
        LocalDate dayOff = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        givenCookies(cookies)
                .body(dayOff)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(204);
    }

    @Test
    @WithMockUser(username = "user")
    void setDayOff_alreadyHasDayOff_returnsBadRequest() {
        LocalDate dayOff = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        user.getDaysOff().add(dayOff);
        userRepository.save(user);

        givenCookies(cookies)
                .body(dayOff)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void removeDayOff_validDayOff_returnsNoContent() {
        LocalDate dayOff = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        user.getDaysOff().add(dayOff);
        user.setDaysOffPerWeek((byte) 1);
        userRepository.save(user);

        givenCookies(cookies)
                .when()
                .queryParam("date", dayOff.toString())
                .delete("/users/me/day-off")
                .then()
                .statusCode(204);
    }

    @Test
    @WithMockUser(username = "user")
    void removeDayOff_today_returnsBadRequest() {
        LocalDate today = LocalDate.now();
        user.getDaysOff().add(today);
        userRepository.save(user);

        givenCookies(cookies)
                .body(today)
                .when()
                .delete("/users/me/day-off")
                .then()
                .statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void setDayOff_hasNoMoreDaysOff_returnsBadRequest() {
        user.setDaysOffPerWeek((byte) 0);
        userRepository.save(user);
        LocalDate dayOff = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        givenCookies(cookies)
                .body(dayOff)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(400);
    }

    @Test
    @WithMockUser(username = "user")
    void patchDayOff_validDateInCurrentWeek_returnsNoContent() {
        LocalDate validDayOff = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        givenCookies(cookies)
                .body(validDayOff)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(204);
    }

    @Test
    @WithMockUser(username = "user")
    void patchDayOff_invalidDateOutsideCurrentWeek_returnsBadRequest() {
        LocalDate invalidDayOff = LocalDate.now().plusWeeks(2);

        givenCookies(cookies)
                .body(invalidDayOff)
                .when()
                .post("/users/me/day-off")
                .then()
                .statusCode(400);
    }
}
