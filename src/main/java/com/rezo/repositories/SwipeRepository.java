package com.rezo.repositories;

import com.rezo.entities.Swipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, UUID> {
    @Query("select s.offer.id from Swipe s where s.user.id = :userId")
    Set<UUID> findOfferIdsByUserId(@Param("userId") UUID userId);
}
