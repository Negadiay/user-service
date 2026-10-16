package com.innowise.userservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.innowise.userservice.TestcontainersConfiguration;
import com.innowise.userservice.config.CacheNames;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected CacheManager cacheManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository cardRepository;

    @BeforeEach
    void cleanUp() {
        cardRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        Cache cache = cacheManager.getCache(CacheNames.USERS);
        if (cache != null) {
            cache.clear();
        }
    }

    protected long createUser(String name, String email) throws Exception {
        String body = """
                {"name": "%s", "surname": "Petrov", "birthDate": "1995-05-20", "email": "%s"}
                """.formatted(name, email);
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    protected long createCard(long userId, String number) throws Exception {
        String response = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(number)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    protected String cardJson(String number) {
        return """
                {"number": "%s", "holder": "IVAN PETROV", "expirationDate": "2029-12-31"}
                """.formatted(number);
    }

    protected boolean isUserCached(long userId) {
        Cache cache = cacheManager.getCache(CacheNames.USERS);
        return cache != null && cache.get(userId) != null;
    }
}