package com.auction.auctionservice.repository;

import com.auction.auctionservice.entity.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuctionRepositoryTest {

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
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "auction");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.schemas", () -> "auction");
        registry.add("spring.flyway.default-schema", () -> "auction");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private LotStatusRepository statusRepository;
    @Autowired
    private LotRepository lotRepository;
    @Autowired
    private LotStatusHistoryRepository historyRepository;

    @Test
    void shouldLoadSeededStatusesAndCategories() {
        assertTrue(categoryRepository.existsByNameIgnoreCase("Electronics"));
        assertTrue(statusRepository.findByCode(LotStatusCode.DRAFT).isPresent());
        assertTrue(statusRepository.findByCode(LotStatusCode.ACTIVE).isPresent());
    }

    @Test
    void shouldFindCategoryByNameIgnoringCase() {
        Optional<Category> category = categoryRepository.findByNameIgnoreCase("electronics");

        assertTrue(category.isPresent());
        assertEquals("Electronics", category.get().getName());
    }

    @Test
    void shouldRejectDuplicateCategoryName() {
        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("Duplicate");

        assertThrows(DataIntegrityViolationException.class, () -> categoryRepository.saveAndFlush(category));
    }

    @Test
    void shouldSaveLotAndSearchByStatusAndCategory() {
        Category category = categoryRepository.findByNameIgnoreCase("Books").orElseThrow();
        LotStatus draft = statusRepository.findByCode(LotStatusCode.DRAFT).orElseThrow();
        Lot lot = buildLot(category, draft, 10L, "Clean Code");
        lotRepository.saveAndFlush(lot);

        List<Lot> result = lotRepository.search(LotStatusCode.DRAFT, category.getId());

        assertEquals(1, result.size());
        assertEquals("Clean Code", result.getFirst().getTitle());
        assertEquals(10L, result.getFirst().getSellerId());
    }

    @Test
    void shouldFindSellerLotsNewestFirst() throws InterruptedException {
        Category category = categoryRepository.findByNameIgnoreCase("Books").orElseThrow();
        LotStatus draft = statusRepository.findByCode(LotStatusCode.DRAFT).orElseThrow();
        lotRepository.saveAndFlush(buildLot(category, draft, 10L, "First lot"));
        Thread.sleep(5);
        lotRepository.saveAndFlush(buildLot(category, draft, 10L, "Second lot"));

        List<Lot> result = lotRepository.findBySellerIdOrderByCreatedAtDesc(10L);

        assertEquals(2, result.size());
        assertEquals("Second lot", result.getFirst().getTitle());
    }

    @Test
    void shouldSaveStatusHistory() {
        Category category = categoryRepository.findByNameIgnoreCase("Books").orElseThrow();
        LotStatus draft = statusRepository.findByCode(LotStatusCode.DRAFT).orElseThrow();
        LotStatus active = statusRepository.findByCode(LotStatusCode.ACTIVE).orElseThrow();
        Lot lot = lotRepository.saveAndFlush(buildLot(category, draft, 10L, "Clean Code"));

        LotStatusHistory history = new LotStatusHistory();
        history.setLot(lot);
        history.setOldStatus(draft);
        history.setNewStatus(active);
        history.setChangedByUserId(10L);
        history.setComment("Activated");

        LotStatusHistory saved = historyRepository.saveAndFlush(history);

        assertNotNull(saved.getId());
        assertEquals("Activated", saved.getComment());
    }

    private Lot buildLot(Category category, LotStatus status, Long sellerId, String title) {
        Lot lot = new Lot();
        lot.setSellerId(sellerId);
        lot.setCategory(category);
        lot.setStatus(status);
        lot.setTitle(title);
        lot.setDescription("Description");
        lot.setStartPrice(BigDecimal.valueOf(100));
        lot.setCurrentPrice(BigDecimal.valueOf(100));
        lot.setBidStep(BigDecimal.TEN);
        lot.setStartTime(LocalDateTime.now());
        lot.setEndTime(LocalDateTime.now().plusDays(1));
        return lot;
    }
}
