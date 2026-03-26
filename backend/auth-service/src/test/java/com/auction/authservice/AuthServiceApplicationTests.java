package com.auction.authservice;

import com.auction.authservice.repository.AuditLogRepository;
import com.auction.authservice.repository.RefreshTokenRepository;
import com.auction.authservice.repository.UserBanRepository;
import com.auction.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class AuthServiceApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private UserBanRepository userBanRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    void contextLoads() {
    }
}
