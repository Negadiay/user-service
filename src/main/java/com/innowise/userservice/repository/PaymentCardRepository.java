package com.innowise.userservice.repository;

import com.innowise.userservice.entity.PaymentCard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {
    List<PaymentCard> findAllByUserId(Long userId);
    boolean existsByNumber(String number);
    boolean existsByNumberAndIdNot(String number, Long id);

    @Query(value = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId", nativeQuery = true)
    long countCardsByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PaymentCard c SET c.active = :active, c.updatedAt = LOCAL DATETIME WHERE c.id = :id")
    int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
}