package com.auction.auctionservice.mapper;

import com.auction.auctionservice.dto.response.*;
import com.auction.auctionservice.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuctionMapper {
    private final JdbcTemplate jdbc;

    public AuctionMapper() {
        this.jdbc = null;
    }

    @Autowired
    public AuctionMapper(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getCreatedAt());
    }

    public LotResponse toLotResponse(Lot l) {
        return new LotResponse(
                l.getId(),
                l.getSellerId(),
                l.getCategory().getId(),
                l.getCategory().getName(),
                l.getStatus().getCode(),
                l.getTitle(),
                l.getDescription(),
                l.getStartPrice(),
                l.getCurrentPrice(),
                l.getBidStep(),
                l.getStartTime(),
                l.getEndTime(),
                l.getWinnerId(),
                l.getCreatedAt(),
                l.getUpdatedAt(),
                userDisplayName(l.getSellerId()),
                userDisplayName(l.getWinnerId()),
                mainImageUrl(l.getId())
        );
    }

    private String userDisplayName(Long userId) {
        if (jdbc == null || userId == null) {
            return null;
        }

        List<String> rows = jdbc.query(
                "select concat_ws(' ', nullif(trim(first_name), ''), nullif(trim(last_name), '')) as name, email from auth.users where id = ? limit 1",
                (rs, rowNum) -> {
                    String name = rs.getString("name");
                    return name == null || name.isBlank() ? rs.getString("email") : name;
                },
                userId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String mainImageUrl(Long lotId) {
        if (jdbc == null || lotId == null) {
            return null;
        }

        List<String> rows = jdbc.query(
                "select image_url from auction.lot_images where lot_id = ? order by is_main desc, id asc limit 1",
                (rs, rowNum) -> rs.getString("image_url"),
                lotId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }
}
