package com.rezo.repositories;

import com.rezo.entities.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID> {
	Optional<School> findByUserId(UUID userId);

	@Modifying
	@Query("delete from School s where s.user.id = :userId")
	int deleteAllByUserId(@Param("userId") UUID userId);
}
