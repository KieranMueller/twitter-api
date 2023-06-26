package com.cooksys.group01.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("validate")
public class ValidateController {

    // Unfinished - Kieran
    @GetMapping("tag/exists/{label}")
    public boolean doesHashtagExist(@PathVariable String label) {
        return false;
    }
}