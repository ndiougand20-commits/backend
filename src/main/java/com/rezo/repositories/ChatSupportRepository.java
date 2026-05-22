package com.rezo.repositories;

import com.rezo.entities.ChatSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatSupportRepository extends JpaRepository<ChatSupport, UUID> {

    @Query("""
	    select c from ChatSupport c
	    where c.user.id = :userId
	      and c.sessionId = :sessionId
	    order by c.createdAt asc
	    """)
    List<ChatSupport> findByUserIdAndSessionIdOrderByCreatedAtAsc(
	    @Param("userId") UUID userId,
	    @Param("sessionId") UUID sessionId
    );

    @Query("""
	    select c from ChatSupport c
	    where c.user.id = :userId
	    order by c.createdAt desc
	    """)
    List<ChatSupport> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
