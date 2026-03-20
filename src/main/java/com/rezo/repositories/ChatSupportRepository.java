package com.rezo.repositories;

import com.rezo.entities.ChatSupport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatSupportRepository extends JpaRepository<ChatSupport, UUID> {
}
