package com.pw.docvault.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("test")
public class Demo {

    @GetMapping
    public ResponseEntity<?> a() {
        return ResponseEntity.ok().body(Map.of("message", "Secured endpoint!"));
    }
}
