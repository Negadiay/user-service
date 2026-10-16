package com.innowise.userservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createUser_shouldReturn201AndUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ivan", "surname": "Petrov", "birthDate": "1995-05-20", "email": "ivan@mail.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("ivan@mail.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.cards", hasSize(0)));
    }

    @Test
    void createUser_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        createUser("Ivan", "ivan@mail.com");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Petr", "surname": "Ivanov", "email": "ivan@mail.com"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_shouldReturn400_whenDataIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "surname": "Petrov", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void getUser_shouldReturn404_whenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getUser_shouldReturnUserWithCardsAndCacheIt() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");
        createCard(userId, "4111111111111111");

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards", hasSize(1)));

        assertThat(isUserCached(userId)).isTrue();
    }

    @Test
    void getAllUsers_shouldFilterByNameWithPagination() throws Exception {
        createUser("Ivan", "ivan@mail.com");
        createUser("Petr", "petr@mail.com");

        mockMvc.perform(get("/api/users")
                        .param("name", "Ivan")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Ivan"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void updateUser_shouldReturnUpdatedUserAndRefreshCache() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");
        mockMvc.perform(get("/api/users/{id}", userId)).andExpect(status().isOk());

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ivan", "surname": "Sidorov", "birthDate": "1995-05-20", "email": "ivan@mail.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Sidorov"));

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(jsonPath("$.surname").value("Sidorov"));
    }

    @Test
    void deactivateUser_shouldReturn204AndEvictCache() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");
        mockMvc.perform(get("/api/users/{id}", userId)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/{id}/deactivate", userId))
                .andExpect(status().isNoContent());

        assertThat(isUserCached(userId)).isFalse();
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteUser_shouldReturn204AndDeleteUserCards() throws Exception {
        long userId = createUser("Ivan", "ivan@mail.com");
        long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/cards/{id}", cardId)).andExpect(status().isNotFound());
    }
}