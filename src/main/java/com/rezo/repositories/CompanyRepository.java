package com.rezo.repositories;

import com.rezo.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
	Optional<Company> findByUserId(UUID userId);

	@Modifying
	@Query("delete from Company c where c.user.id = :userId")
	int deleteAllByUserId(@Param("userId") UUID userId);
}
