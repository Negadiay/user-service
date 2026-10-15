package com.innowise.userservice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.innowise.userservice.exception.CardLimitExceededException;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void addCard_shouldLinkCardToUser() {
        User user = new User();
        PaymentCard card = new PaymentCard();

        user.addCard(card);

        assertThat(user.getCards()).containsExactly(card);
        assertThat(card.getUser()).isSameAs(user);
    }

    @Test
    void addCard_shouldThrow_whenUserAlreadyHasMaxCards() {
        User user = new User();
        for (int i = 0; i < User.MAX_CARDS; i++) {
            user.addCard(new PaymentCard());
        }

        assertThatThrownBy(() -> user.addCard(new PaymentCard()))
                .isInstanceOf(CardLimitExceededException.class);
        assertThat(user.getCards()).hasSize(User.MAX_CARDS);
    }

    @Test
    void removeCard_shouldUnlinkCardFromUser() {
        User user = new User();
        PaymentCard card = new PaymentCard();
        user.addCard(card);

        user.removeCard(card);

        assertThat(user.getCards()).isEmpty();
        assertThat(card.getUser()).isNull();
    }
}