package com.lyncalor.calyra.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "pending_draft", indexes = {
        @Index(name = "idx_pending_draft_chat_id", columnList = "chat_id")
})
public class PendingDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private long chatId;

    @Lob
    @Column(name = "raw_initial_text", nullable = false)
    private String rawInitialText;

    @Lob
    @Column(name = "draft_json", nullable = false)
    private String draftJson;

    @Lob
    @Column(name = "suggested_json")
    private String suggestedJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PendingDraftStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public Long getId() {
        return id;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getRawInitialText() {
        return rawInitialText;
    }

    public void setRawInitialText(String rawInitialText) {
        this.rawInitialText = rawInitialText;
    }

    public String getDraftJson() {
        return draftJson;
    }

    public void setDraftJson(String draftJson) {
        this.draftJson = draftJson;
    }

    public String getSuggestedJson() {
        return suggestedJson;
    }

    public void setSuggestedJson(String suggestedJson) {
        this.suggestedJson = suggestedJson;
    }

    public PendingDraftStatus getStatus() {
        return status;
    }

    public void setStatus(PendingDraftStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
