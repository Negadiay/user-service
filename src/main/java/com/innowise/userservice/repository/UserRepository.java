package com.innowise.userservice.repository;

import com.innowise.userservice.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("Select u FROM User u LEFT JOIN FETCH u.cards WHERE u.id = :id")
    Optional<User> findByIdWithCards(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.active = :active, u.updatedAt = LOCAL DATETIME WHERE u.id = :id")
    int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
}
