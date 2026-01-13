package com.lyncalor.calyra.schedule;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleDraftValidationTest {

    private final Validator validator;

    ScheduleDraftValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void requiresStartWhenNoClarification() {
        ScheduleDraft draft = new ScheduleDraft(
                "Title",
                null,
                null,
                null,
                null,
                null,
                "Europe/Berlin",
                30,
                false,
                null
        );

        assertThat(validator.validate(draft)).isNotEmpty();
    }

    @Test
    void endMustBeAfterStart() {
        OffsetDateTime start = OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2025, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ScheduleDraft draft = new ScheduleDraft(
                "Title",
                start,
                end,
                null,
                null,
                null,
                "Europe/Berlin",
                30,
                false,
                null
        );

        assertThat(validator.validate(draft)).isNotEmpty();
    }
}
