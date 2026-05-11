package com.auction.biddingservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=change-me-please-change-me-please-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=bidding",
        "spring.flyway.enabled=true",
        "spring.flyway.schemas=bidding",
        "spring.flyway.default-schema=bidding",
        "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureMockMvc
@Testcontainers
class BiddingPublicApiIntegrationTest {

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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void createAuctionLotSnapshot() {
        jdbc.execute("create schema if not exists auction");
        jdbc.execute("""
                create table if not exists auction.lots (
                    id bigint primary key,
                    seller_id bigint not null,
                    status_id bigint not null,
                    current_price numeric(12, 2) not null,
                    bid_step numeric(12, 2) not null,
                    end_time timestamp not null
                )
                """);
        jdbc.update("""
                insert into auction.lots (id, seller_id, status_id, current_price, bid_step, end_time)
                values (100, 10, 1, 100.00, 10.00, current_timestamp + interval '1 day')
                on conflict (id) do nothing
                """);
    }

    @Test
    void anonymousUsersCanReadBidHistoryButCannotPlaceBid() throws Exception {
        mockMvc.perform(get("/bids/lots/100"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lotId": 100,
                                  "amount": 120
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
