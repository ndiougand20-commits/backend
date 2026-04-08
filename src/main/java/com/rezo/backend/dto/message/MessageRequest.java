package com.rezo.backend.dto.message;

import java.util.UUID;

public class MessageRequest {

    private UUID receiverId;
    private String content;
    private UUID relatedOfferId;

    public UUID getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(UUID receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getRelatedOfferId() {
        return relatedOfferId;
    }

    public void setRelatedOfferId(UUID relatedOfferId) {
        this.relatedOfferId = relatedOfferId;
    }
}
