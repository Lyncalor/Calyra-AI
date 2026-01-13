package com.lyncalor.calyra.preference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "event_stat", indexes = {
        @Index(name = "idx_event_stat_chat_id", columnList = "chat_id"),
        @Index(name = "idx_event_stat_chat_type", columnList = "chat_id,type")
})
public class EventStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private long chatId;

    @Column(name = "type")
    private String type;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "location")
    private String location;

    @Column(name = "remind_minutes")
    private Integer remindMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getRemindMinutes() {
        return remindMinutes;
    }

    public void setRemindMinutes(Integer remindMinutes) {
        this.remindMinutes = remindMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
