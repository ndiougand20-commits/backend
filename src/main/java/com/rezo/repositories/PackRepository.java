package com.rezo.repositories;

import com.rezo.entities.Pack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PackRepository extends JpaRepository<Pack, UUID> {
	Optional<Pack> findByNomIgnoreCase(String nom);
}
