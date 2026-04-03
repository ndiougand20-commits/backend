package com.rezo.repositories;

import com.rezo.entities.Pack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PackRepository extends JpaRepository<Pack, UUID> {
	Optional<Pack> findByNomIgnoreCase(String nom);

	@Query("select count(u) from User u where u.pack.id = :packId")
	long countUsersByPackId(@Param("packId") UUID packId);
}
