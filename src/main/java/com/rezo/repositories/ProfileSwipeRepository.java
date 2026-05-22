package com.rezo.repositories;

import com.rezo.entities.ProfileSwipe;
import com.rezo.entities.enums.SwipeAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileSwipeRepository extends JpaRepository<ProfileSwipe, UUID> {

    @Query("select ps from ProfileSwipe ps where ps.swiper.id = :swiperId and ps.targetUser.id = :targetUserId")
    Optional<ProfileSwipe> findBySwiperIdAndTargetUserId(
            @Param("swiperId") UUID swiperId,
            @Param("targetUserId") UUID targetUserId
    );

    @Query("select ps.targetUser.id from ProfileSwipe ps where ps.swiper.id = :swiperId")
    List<UUID> findTargetUserIdsBySwiperId(@Param("swiperId") UUID swiperId);

    @Query("select ps.targetUser.id from ProfileSwipe ps where ps.swiper.id = :swiperId and ps.action = :action")
    List<UUID> findTargetUserIdsBySwiperIdAndAction(
            @Param("swiperId") UUID swiperId,
            @Param("action") SwipeAction action
    );

    @Query("select ps from ProfileSwipe ps where ps.targetUser.id = :targetUserId")
    List<ProfileSwipe> findByTargetUserId(@Param("targetUserId") UUID targetUserId);

    @Query("select ps from ProfileSwipe ps join fetch ps.targetUser where ps.swiper.id = :swiperId and ps.action = :action")
    List<ProfileSwipe> findBySwiperIdAndAction(
            @Param("swiperId") UUID swiperId,
            @Param("action") SwipeAction action
    );

    @Query("""
            select count(ps) > 0 from ProfileSwipe ps
            where ps.swiper.id = :swiperId
              and ps.targetUser.id = :targetUserId
              and ps.action = :action
            """)
    boolean existsBySwiperIdAndTargetUserIdAndAction(
            @Param("swiperId") UUID swiperId,
            @Param("targetUserId") UUID targetUserId,
            @Param("action") SwipeAction action
    );

    @Query("select ps from ProfileSwipe ps where ps.targetUser.id = :targetUserId and ps.action = :action")
    List<ProfileSwipe> findByTargetUserIdAndAction(
            @Param("targetUserId") UUID targetUserId,
            @Param("action") SwipeAction action
    );

    @Query("select count(ps) from ProfileSwipe ps where ps.swiper.id = :swiperId and ps.action = :action")
    long countBySwiperIdAndAction(
            @Param("swiperId") UUID swiperId,
            @Param("action") SwipeAction action
    );

    @Query("select count(ps) from ProfileSwipe ps where ps.targetUser.id = :targetUserId and ps.action = :action")
    long countByTargetUserIdAndAction(
            @Param("targetUserId") UUID targetUserId,
            @Param("action") SwipeAction action
    );
}
