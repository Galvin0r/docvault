package com.pw.docvault.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("aboba")
public class Demo {

    @PostMapping
    public ResponseEntity<?> a() {
        return ResponseEntity.ok().build();
    }
}
