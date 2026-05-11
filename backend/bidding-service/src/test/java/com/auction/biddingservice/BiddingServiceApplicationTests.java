package com.auction.biddingservice;

import com.auction.biddingservice.repository.BidRepository;
import com.auction.biddingservice.repository.BidResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "jwt.secret=change-me-please-change-me-please-1",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class BiddingServiceApplicationTests {

    @MockBean
    private BidRepository bidRepository;

    @MockBean
    private BidResultRepository bidResultRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }
}
