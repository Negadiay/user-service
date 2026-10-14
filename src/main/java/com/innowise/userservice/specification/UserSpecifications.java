package com.innowise.userservice.specification;

import com.innowise.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {
    private UserSpecifications() {
    }

    public static Specification<User> hasName(String name) {
        return (root, query, cb) -> name == null || name.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("name"), name);
    }

    public static Specification<User> hasSurname(String surname) {
        return (root, query, cb) -> surname == null || surname.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("surname"), surname);
    }

    public static Specification<User> byNameAndSurname(String name, String surname) {
        return hasName(name).and(hasSurname(surname));
    }
}