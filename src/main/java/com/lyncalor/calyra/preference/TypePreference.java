package com.lyncalor.calyra.preference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "type_preference",
        indexes = @Index(name = "idx_type_pref_chat_id", columnList = "chat_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_type_pref_chat_type", columnNames = {"chat_id", "event_type"}))
public class TypePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private long chatId;

    @Column(name = "event_type", nullable = false)
    private String type;

    @Column(name = "typical_duration_minutes")
    private Integer typicalDurationMinutes;

    @Column(name = "default_location")
    private String defaultLocation;

    @Column(name = "default_remind_minutes")
    private Integer defaultRemindMinutes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTypicalDurationMinutes() {
        return typicalDurationMinutes;
    }

    public void setTypicalDurationMinutes(Integer typicalDurationMinutes) {
        this.typicalDurationMinutes = typicalDurationMinutes;
    }

    public String getDefaultLocation() {
        return defaultLocation;
    }

    public void setDefaultLocation(String defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public Integer getDefaultRemindMinutes() {
        return defaultRemindMinutes;
    }

    public void setDefaultRemindMinutes(Integer defaultRemindMinutes) {
        this.defaultRemindMinutes = defaultRemindMinutes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
