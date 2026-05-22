package com.rezo.repositories;

import com.rezo.entities.Swipe;
import com.rezo.entities.User;
import com.rezo.entities.enums.SwipeAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, UUID> {

    @Query("select s.offer.id from Swipe s where s.user.id = :userId")
    Set<UUID> findOfferIdsByUserId(@Param("userId") UUID userId);

    @Query("select s from Swipe s where s.user.id = :userId and s.offer.id = :offerId")
    Optional<Swipe> findByUserIdAndOfferId(@Param("userId") UUID userId, @Param("offerId") UUID offerId);

    @Query("select u from Swipe s join s.user u where s.offer.id = :offerId and s.action = :action")
    List<User> findLikersByOfferId(@Param("offerId") UUID offerId, @Param("action") SwipeAction action);

        @Query("""
            select s from Swipe s
            join fetch s.offer o
            left join fetch o.ownerEntreprise oe
            left join fetch oe.user
            left join fetch o.ownerEcole se
            left join fetch se.user
            where s.user.id = :userId and s.action = :action
            """)
        List<Swipe> findByUserIdAndActionWithOffer(@Param("userId") UUID userId, @Param("action") SwipeAction action);

        @Query("""
            select count(s) > 0 from Swipe s
            join s.offer o
            left join o.ownerEntreprise oe
            left join o.ownerEcole se
            where s.user.id = :candidateUserId
              and s.action = :action
              and (oe.user.id = :ownerUserId or se.user.id = :ownerUserId)
            """)
        boolean existsCandidateLikeOnOwnerOffers(
            @Param("candidateUserId") UUID candidateUserId,
            @Param("ownerUserId") UUID ownerUserId,
            @Param("action") SwipeAction action
        );

        @Query("""
            select o.titre from Swipe s
            join s.offer o
            left join o.ownerEntreprise oe
            left join o.ownerEcole se
            where s.user.id = :candidateUserId
              and s.action = :action
              and (oe.user.id = :ownerUserId or se.user.id = :ownerUserId)
            order by s.createdAt desc
            """)
        List<String> findCandidateLikedOfferTitlesForOwner(
            @Param("candidateUserId") UUID candidateUserId,
            @Param("ownerUserId") UUID ownerUserId,
            @Param("action") SwipeAction action
        );

        @Query("select count(s) from Swipe s where s.user.id = :userId and s.action = :action")
        long countByUserIdAndAction(@Param("userId") UUID userId, @Param("action") SwipeAction action);

        @Query("""
                        select count(s) from Swipe s
                        join s.offer o
                        left join o.ownerEntreprise oe
                        left join o.ownerEcole se
                        where s.action = :action
                            and (oe.user.id = :ownerUserId or se.user.id = :ownerUserId)
                        """)
        long countLikesOnOwnerOffers(@Param("ownerUserId") UUID ownerUserId, @Param("action") SwipeAction action);
}
