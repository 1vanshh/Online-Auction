package com.auction.authservice.repository;

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
        User user = buildUser("Ivan", "Ivanov", "ivan@example.com");

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals("Ivan", savedUser.getFirstName());
        assertEquals("ivan@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    @DisplayName("Should find user by id")
    void shouldFindUserById() {
        User savedUser = userRepository.save(buildUser("Anna", "Petrova", "anna@example.com"));

        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals("anna@example.com", foundUser.get().getEmail());
    }

    @Test
    @DisplayName("Should return empty optional when user not found by id")
    void shouldReturnEmptyWhenUserNotFoundById() {
        Optional<User> foundUser = userRepository.findById(999999L);

        assertTrue(foundUser.isEmpty());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        userRepository.save(buildUser("Petr", "Sidorov", "petr@example.com"));

        Optional<User> foundUser = userRepository.findByEmail("petr@example.com");

        assertTrue(foundUser.isPresent());
        assertEquals("Petr", foundUser.get().getFirstName());
        assertEquals("petr@example.com", foundUser.get().getEmail());
    }

    @Test
    @DisplayName("Should return empty optional when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        Optional<User> foundUser = userRepository.findByEmail("missing@example.com");

        assertTrue(foundUser.isEmpty());
    }

    @Test
    @DisplayName("Should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        userRepository.save(buildUser("Maria", "Smirnova", "maria@example.com"));

        boolean exists = userRepository.existsByEmail("maria@example.com");

        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("ghost@example.com");

        assertFalse(exists);
    }

    @Test
    @DisplayName("Should return all saved users")
    void shouldReturnAllUsers() {
        userRepository.save(buildUser("User1", "Test", "user1@example.com"));
        userRepository.save(buildUser("User2", "Test", "user2@example.com"));

        List<User> users = userRepository.findAll();

        assertEquals(2, users.size());
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        User savedUser = userRepository.save(buildUser("Delete", "Me", "delete@example.com"));

        userRepository.delete(savedUser);

        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertTrue(foundUser.isEmpty());
    }

    @Test
    @DisplayName("Should update existing user")
    void shouldUpdateUser() {
        User savedUser = userRepository.save(buildUser("OldName", "User", "update@example.com"));

        savedUser.setFirstName("NewName");
        User updatedUser = userRepository.save(savedUser);

        Optional<User> foundUser = userRepository.findById(updatedUser.getId());

        assertTrue(foundUser.isPresent());
        assertEquals("NewName", foundUser.get().getFirstName());
        assertEquals("update@example.com", foundUser.get().getEmail());
    }

    @Test
    @DisplayName("Should throw exception when saving user with duplicate email")
    void shouldThrowExceptionWhenSavingDuplicateEmail() {
        userRepository.saveAndFlush(buildUser("First", "User", "duplicate@example.com"));

        User duplicateUser = buildUser("Second", "User", "duplicate@example.com");

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(duplicateUser));
    }

    private User buildUser(String firstName, String lastName, String email) {
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
        user.setActive(true);
        user.setBanned(false);
        return user;
    }
}
