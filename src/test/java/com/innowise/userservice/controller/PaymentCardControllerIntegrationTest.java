package com.innowise.userservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.innowise.userservice.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createCard_shouldReturn201AndCard() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createCard_shouldReturn409_whenUserHasMaxCards() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");
        for (int i = 0; i < User.MAX_CARDS; i++) {
            createCard(userId, "411111111111111" + i);
        }

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111119")))
                .andExpect(status().isConflict());
    }

    @Test
    void createCard_shouldReturn409_whenNumberAlreadyExists() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");
        createCard(userId, "4111111111111111");

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isConflict());
    }

    @Test
    void createCard_shouldReturn400_whenNumberIsInvalid() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.number").exists());
    }

    @Test
    void createCard_shouldReturn404_whenUserNotFound() throws Exception {
        mockMvc.perform(post("/api/users/{userId}/cards", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCard_shouldEvictOwnerCache() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");
        mockMvc.perform(get("/api/users/{id}", userId)).andExpect(status().isOk());
        assertThat(isUserCached(userId)).isTrue();

        createCard(userId, "4111111111111111");

        assertThat(isUserCached(userId)).isFalse();
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(jsonPath("$.cards", hasSize(1)));
    }

    @Test
    void getCardsByUser_shouldReturnAllUserCards() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");
        createCard(userId, "4111111111111111");
        createCard(userId, "4111111111111112");

        mockMvc.perform(get("/api/users/{userId}/cards", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAllCards_shouldFilterByOwnerName() throws Exception {
        long ivanId = createUser("Ivan", "ivan@mail.com");
        long petrId = createUser("Petr", "petr@mail.com");
        createCard(ivanId, "4111111111111111");
        createCard(petrId, "4111111111111112");

        mockMvc.perform(get("/api/cards").param("name", "Petr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId").value(petrId));
    }

    @Test
    void deactivateCard_shouldReturn204AndMarkCardInactive() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");
        long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(patch("/api/cards/{id}/deactivate", cardId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(jsonPath("$.active").value(false));
    }
}