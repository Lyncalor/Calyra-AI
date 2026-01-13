package com.lyncalor.calyra.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public record ScheduleDraft(
        @NotBlank String title,
        OffsetDateTime start,
        OffsetDateTime end,
        String type,
        String location,
        String notes,
        @NotBlank String timezone,
        Integer remindMinutesBefore,
        boolean needsClarification,
        List<String> clarificationQuestions
) {
    @JsonCreator
    public ScheduleDraft(
            @JsonProperty("title") String title,
            @JsonProperty("start") OffsetDateTime start,
            @JsonProperty("end") OffsetDateTime end,
            @JsonProperty("type") String type,
            @JsonProperty("location") String location,
            @JsonProperty("notes") String notes,
            @JsonProperty("timezone") String timezone,
            @JsonProperty("remindMinutesBefore") Integer remindMinutesBefore,
            @JsonProperty("needsClarification") boolean needsClarification,
            @JsonProperty("clarificationQuestions") List<String> clarificationQuestions
    ) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.type = (type == null || type.isBlank()) ? "Other" : type;
        this.location = location;
        this.notes = notes;
        this.timezone = (timezone == null || timezone.isBlank()) ? "Europe/Berlin" : timezone;
        this.remindMinutesBefore = remindMinutesBefore == null ? 30 : remindMinutesBefore;
        this.needsClarification = needsClarification;
        this.clarificationQuestions = clarificationQuestions == null ? new ArrayList<>() : clarificationQuestions;
    }

    @AssertTrue(message = "start must be provided when needsClarification=false")
    public boolean isStartPresentIfNoClarification() {
        if (needsClarification) {
            return true;
        }
        return start != null;
    }

    @AssertTrue(message = "end must be after start when end is provided")
    public boolean isEndAfterStart() {
        if (start == null || end == null) {
            return true;
        }
        return end.isAfter(start);
    }
}
