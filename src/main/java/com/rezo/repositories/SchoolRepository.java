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

	@Query("select s from School s join fetch s.user where s.id = :schoolId")
	Optional<School> findByIdWithUser(@Param("schoolId") UUID schoolId);

	@Query("select s.user.id from School s where s.id = :schoolId")
	Optional<UUID> findOwnerUserIdById(@Param("schoolId") UUID schoolId);

	@Query("select s from School s join fetch s.user")
	java.util.List<School> findAllWithUser();

	@Modifying
	@Query("delete from School s where s.id = :schoolId")
	int deleteByIdDirect(@Param("schoolId") UUID schoolId);

	@Modifying
	@Query("delete from School s where s.user.id = :userId")
	int deleteAllByUserId(@Param("userId") UUID userId);
}
