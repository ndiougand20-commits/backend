package com.rezo.repositories;

import com.rezo.entities.UserMediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserMediaFileRepository extends JpaRepository<UserMediaFile, UUID> {

    List<UserMediaFile> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserMediaFile> findByUserIdAndCategoryOrderByCreatedAtDesc(UUID userId, String category);

    @Modifying
    @Query("delete from UserMediaFile f where f.id = :fileId and f.userId = :userId")
    int deleteByIdAndUserId(@Param("fileId") UUID fileId, @Param("userId") UUID userId);

    @Modifying
    @Query("delete from UserMediaFile f where f.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
