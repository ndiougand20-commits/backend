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

	@Query("select c from Company c join fetch c.user where c.id = :companyId")
	Optional<Company> findByIdWithUser(@Param("companyId") UUID companyId);

	@Query("select c.user.id from Company c where c.id = :companyId")
	Optional<UUID> findOwnerUserIdById(@Param("companyId") UUID companyId);

	@Query("select c from Company c join fetch c.user")
	java.util.List<Company> findAllWithUser();

	@Modifying
	@Query("delete from Company c where c.id = :companyId")
	int deleteByIdDirect(@Param("companyId") UUID companyId);

	@Modifying
	@Query("delete from Company c where c.user.id = :userId")
	int deleteAllByUserId(@Param("userId") UUID userId);
}
