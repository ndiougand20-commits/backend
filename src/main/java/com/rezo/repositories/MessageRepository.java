package com.rezo.repositories;

import com.rezo.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select m from Message m
            left join fetch m.sender s
            left join fetch m.receiver r
            left join fetch m.offer o
            where ((s.id = :firstUserId and r.id = :secondUserId)
                or (s.id = :secondUserId and r.id = :firstUserId))
            order by m.createdAt asc
            """)
    List<Message> findConversation(@Param("firstUserId") UUID firstUserId, @Param("secondUserId") UUID secondUserId);

    @Query("""
            select m from Message m
            left join fetch m.sender s
            left join fetch m.receiver r
            left join fetch m.offer o
            where (s.id = :userId or r.id = :userId)
              and (:read is null or m.isRead = :read)
            order by m.createdAt desc
            """)
    List<Message> findAllForUser(@Param("userId") UUID userId, @Param("read") Boolean read);

    @Query("""
            select m from Message m
            left join fetch m.sender s
            left join fetch m.receiver r
            left join fetch m.offer o
            where m.id = :messageId
            """)
    Optional<Message> findByIdWithUsersAndOffer(@Param("messageId") UUID messageId);

    @Modifying
    @Query("delete from Message m where m.id = :messageId and m.sender.id = :senderId")
    int deleteByIdAndSenderId(@Param("messageId") UUID messageId, @Param("senderId") UUID senderId);
}
