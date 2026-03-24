package com.auction.authservice.dto.request;

import com.auction.authservice.entity.Role;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {

    private Boolean active;
    private Boolean banned;
    private Role role;
}
