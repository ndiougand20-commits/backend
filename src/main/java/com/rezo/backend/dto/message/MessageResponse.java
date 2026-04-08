package com.rezo.backend.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rezo.entities.Message;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageResponse {

    private final UUID id;
    private final UUID senderId;
    private final String senderDisplayName;
    private final UUID receiverId;
    private final String receiverDisplayName;
    private final String content;
    private final boolean isRead;
    private final UUID relatedOfferId;
    private final LocalDateTime createdAt;

    public MessageResponse(
            UUID id,
            UUID senderId,
            String senderDisplayName,
            UUID receiverId,
            String receiverDisplayName,
            String content,
            boolean isRead,
            UUID relatedOfferId,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.senderId = senderId;
        this.senderDisplayName = senderDisplayName;
        this.receiverId = receiverId;
        this.receiverDisplayName = receiverDisplayName;
        this.content = content;
        this.isRead = isRead;
        this.relatedOfferId = relatedOfferId;
        this.createdAt = createdAt;
    }

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender() != null ? message.getSender().getId() : null,
                displayName(message.getSender()),
                message.getReceiver() != null ? message.getReceiver().getId() : null,
                displayName(message.getReceiver()),
                message.getContent(),
                message.isRead(),
                message.getOffer() != null ? message.getOffer().getId() : null,
                message.getCreatedAt()
        );
    }

    private static String displayName(com.rezo.entities.User user) {
        if (user == null) {
            return null;
        }
        String prenom = user.getPrenom() != null ? user.getPrenom().trim() : "";
        String nom = user.getNom() != null ? user.getNom().trim() : "";
        String fullName = (prenom + " " + nom).trim();
        return fullName.isBlank() ? user.getEmail() : fullName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public String getReceiverDisplayName() {
        return receiverDisplayName;
    }

    public String getContent() {
        return content;
    }

    @JsonProperty("isRead")
    public boolean isRead() {
        return isRead;
    }

    public UUID getRelatedOfferId() {
        return relatedOfferId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
