package com.pw.docvault.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder()
                                                        .addModule(new JavaTimeModule())
                                                        .build();

    @RestController
    static class ThrowingController {
        @GetMapping("/throw/app")
        public String throwApp() {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND, "no group");
        }
        @GetMapping("/throw/generic")
        public String throwGeneric() {
            throw new RuntimeException("kaboom");
        }
    }

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void appException_isMappedWithItsStatusAndBody() throws Exception {
        mockMvc.perform(get("/throw/app"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.value()))
               .andExpect(jsonPath("$.message").value("no group"))
               .andExpect(jsonPath("$.status").value(NOT_FOUND.value()))
               .andExpect(jsonPath("$.path").value("/throw/app"))
               .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void genericException_isMappedWith500AndGenericBody() throws Exception {
        mockMvc.perform(get("/throw/generic"))
               .andExpect(status().isInternalServerError())
               .andExpect(jsonPath("$.code").value(ErrorCode.UNKNOWN.value()))
               .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
               .andExpect(jsonPath("$.status").value(INTERNAL_SERVER_ERROR.value()))
               .andExpect(jsonPath("$.path").value("/throw/generic"))
               .andExpect(jsonPath("$.timestamp").exists());
    }
}
