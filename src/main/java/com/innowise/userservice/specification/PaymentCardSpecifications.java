package com.innowise.userservice.specification;

import com.innowise.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public final class PaymentCardSpecifications {
    private PaymentCardSpecifications() {
    }

    public static Specification<PaymentCard> hasUserName(String name) {
        return (root, query, cb) -> name == null || name.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("user").get("name"), name);
    }

    public static Specification<PaymentCard> hasUserSurname(String surname) {
        return (root, query, cb) -> surname == null || surname.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("user").get("surname"), surname);
    }

    public static Specification<PaymentCard> byUserNameAndSurname(String name, String surname) {
        return hasUserName(name).and(hasUserSurname(surname));
    }
}