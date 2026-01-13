package com.lyncalor.calyra.preference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EventStatRepository extends JpaRepository<EventStat, Long> {

    List<EventStat> findByChatIdAndCreatedAtAfter(long chatId, Instant after);

    List<EventStat> findByChatIdAndTypeAndCreatedAtAfter(long chatId, String type, Instant after);
}
