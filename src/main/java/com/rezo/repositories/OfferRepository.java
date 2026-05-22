package com.rezo.repositories;

import com.rezo.entities.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {
    @Query("""
            select distinct o from Offer o
            left join fetch o.ownerEntreprise oe
            left join fetch oe.user
            left join fetch o.ownerEcole se
            left join fetch se.user
            """)
    List<Offer> findAllWithOwners();

    @Query("""
            select o from Offer o
            left join fetch o.ownerEntreprise oe
            left join fetch oe.user
            left join fetch o.ownerEcole se
            left join fetch se.user
            where o.id = :offerId
            """)
    Optional<Offer> findByIdWithOwners(@Param("offerId") UUID offerId);

    @Query("""
            select coalesce(oe.user.id, se.user.id) from Offer o
            left join o.ownerEntreprise oe
            left join o.ownerEcole se
            where o.id = :offerId
            """)
    Optional<UUID> findOwnerUserIdById(@Param("offerId") UUID offerId);

    @Modifying
    @Query("delete from Offer o where o.id = :offerId")
    int deleteByIdDirect(@Param("offerId") UUID offerId);

        @Query("""
                        select count(o) from Offer o
                        left join o.ownerEntreprise oe
                        left join o.ownerEcole se
                        where oe.user.id = :ownerUserId or se.user.id = :ownerUserId
                        """)
        long countByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);
}
