package com.cooksys.group01.entity.embeddable;

import jakarta.persistence.Embeddable;

@Embeddable
public class Credentials {

    private String username;
    private String password;
}