package com.rezo.repositories;

import com.rezo.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
	Optional<Profile> findByUserId(UUID userId);

	@Modifying
	@Query("delete from Profile p where p.user.id = :userId")
	int deleteAllByUserId(@Param("userId") UUID userId);
}
