package com.auction.authservice.repository;

import com.auction.authservice.entity.Role;
import com.auction.authservice.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.4")
            .withDatabaseName("online_auction_test")
            .withUsername("postgres")
            .withPassword("1234");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "auth");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.schemas", () -> "auth");
        registry.add("spring.flyway.default-schema", () -> "auth");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save user and generate id")
    void shouldSaveUserAndGenerateId() {
        User savedUser = userRepository.save(buildUser("Ivan", "Ivanov", "ivan@example.com", true, false));

        assertNotNull(savedUser.getId());
        assertEquals("Ivan", savedUser.getFirstName());
        assertEquals(Role.USER, savedUser.getRole());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    void shouldFindByEmail() {
        userRepository.save(buildUser("Anna", "Petrova", "anna@example.com", true, false));

        Optional<User> found = userRepository.findByEmail("anna@example.com");

        assertTrue(found.isPresent());
        assertEquals("Anna", found.get().getFirstName());
    }

    @Test
    void shouldReturnEmptyWhenEmailMissing() {
        assertTrue(userRepository.findByEmail("missing@example.com").isEmpty());
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        userRepository.save(buildUser("Petr", "Sidorov", "petr@example.com", true, false));

        assertTrue(userRepository.existsByEmail("petr@example.com"));
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        assertFalse(userRepository.existsByEmail("ghost@example.com"));
    }

    @Test
    void shouldUpdateExistingUser() {
        User saved = userRepository.save(buildUser("Old", "Name", "update@example.com", true, false));
        saved.setFirstName("New");

        User updated = userRepository.saveAndFlush(saved);

        assertEquals("New", userRepository.findById(updated.getId()).orElseThrow().getFirstName());
    }

    @Test
    void shouldDeleteUser() {
        User saved = userRepository.save(buildUser("Delete", "Me", "delete@example.com", true, false));

        userRepository.delete(saved);
        userRepository.flush();

        assertTrue(userRepository.findById(saved.getId()).isEmpty());
    }

    @Test
    void shouldThrowForDuplicateEmail() {
        userRepository.saveAndFlush(buildUser("First", "User", "duplicate@example.com", true, false));

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(buildUser("Second", "User", "duplicate@example.com", true, false)));
    }

    @Test
    void shouldFindActiveUserById() {
        User saved = userRepository.save(buildUser("Active", "User", "active@example.com", true, false));

        Optional<User> found = userRepository.findByIdAndActiveTrue(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("active@example.com", found.get().getEmail());
    }

    @Test
    void shouldNotFindInactiveUserByIdAndActiveTrue() {
        User saved = userRepository.save(buildUser("Inactive", "User", "inactive@example.com", false, false));

        assertTrue(userRepository.findByIdAndActiveTrue(saved.getId()).isEmpty());
    }

    @Test
    void shouldReturnOnlyActiveUsers() {
        userRepository.save(buildUser("User1", "Test", "user1@example.com", true, false));
        userRepository.save(buildUser("User2", "Test", "user2@example.com", false, false));
        userRepository.save(buildUser("User3", "Test", "user3@example.com", true, true));

        List<User> users = userRepository.findAllByActiveTrue();

        assertEquals(2, users.size());
        assertTrue(users.stream().allMatch(User::isActive));
    }

    private User buildUser(String firstName, String lastName, String email, boolean active, boolean banned) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash("hashed_password_123");
        user.setPhone("+79991234567");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setCountry("Russia");
        user.setCity("Moscow");
        user.setAddressLine("Tverskaya 1");
        user.setPostalCode("125009");
        user.setRole(Role.USER);
        user.setActive(active);
        user.setBanned(banned);
        return user;
    }
}
