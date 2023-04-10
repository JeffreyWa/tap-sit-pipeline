package com.example.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
class HelloControllerTest {

    @Autowired
    private HelloController controller;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void index() throws Exception {
        //assertEquals("Greetings from Spring Boot2 + Tanzu!", controller.index());
        String testurl = System.getProperty("url", "/");
        System.out.println(testurl);
        mockMvc
            .perform(get(testurl))
            .andExpect(status().isOk())
            .andExpect(content().string("Greetings from Spring Boot2 + Tanzu!"));
    }
}
