package com.auction.authservice.dto.request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 30)
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone format")
    private String phone;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String city;

    @Size(max = 255)
    private String addressLine;

    @Size(max = 20)
    private String postalCode;
}
