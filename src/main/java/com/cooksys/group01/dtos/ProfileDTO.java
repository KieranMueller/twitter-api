package com.cooksys.group01.dtos;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

}
