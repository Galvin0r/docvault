package com.pw.docvault.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("aboba")
public class Demo {

    @PostMapping
    public ResponseEntity<?> a() {
        return ResponseEntity.ok().build();
    }
}
