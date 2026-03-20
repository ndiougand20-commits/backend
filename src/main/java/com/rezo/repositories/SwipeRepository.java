package com.rezo.repositories;

import com.rezo.entities.Swipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, UUID> {
}
