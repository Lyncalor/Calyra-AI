package com.lyncalor.calyra.preference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "user_preference", indexes = {
        @Index(name = "idx_user_pref_chat_id", columnList = "chat_id")
})
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private long chatId;

    @Column(name = "default_remind_minutes")
    private Integer defaultRemindMinutes;

    @Column(name = "default_timezone", nullable = false)
    private String defaultTimezone = "Europe/Berlin";

    @Column(name = "working_hours_start")
    private LocalTime workingHoursStart;

    @Column(name = "working_hours_end")
    private LocalTime workingHoursEnd;

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

    public Integer getDefaultRemindMinutes() {
        return defaultRemindMinutes;
    }

    public void setDefaultRemindMinutes(Integer defaultRemindMinutes) {
        this.defaultRemindMinutes = defaultRemindMinutes;
    }

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public void setDefaultTimezone(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    public LocalTime getWorkingHoursStart() {
        return workingHoursStart;
    }

    public void setWorkingHoursStart(LocalTime workingHoursStart) {
        this.workingHoursStart = workingHoursStart;
    }

    public LocalTime getWorkingHoursEnd() {
        return workingHoursEnd;
    }

    public void setWorkingHoursEnd(LocalTime workingHoursEnd) {
        this.workingHoursEnd = workingHoursEnd;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
