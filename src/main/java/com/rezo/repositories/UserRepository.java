package com.rezo.repositories;

import com.rezo.entities.User;
import com.rezo.entities.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select u from User u left join fetch u.pack where u.id = :userId")
    Optional<User> findByIdWithPack(@Param("userId") UUID userId);

    @Query("select u.id from User u where lower(u.email) = lower(:email)")
    Optional<UUID> findIdByEmail(@Param("email") String email);

    List<User> findByRoleIn(Collection<UserRole> roles);

    @Modifying
    @Query("delete from User u where u.id = :userId")
    int deleteByIdDirect(@Param("userId") UUID userId);
}
