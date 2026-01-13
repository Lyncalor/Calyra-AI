package com.lyncalor.calyra.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DedupeEntryRepository extends JpaRepository<DedupeEntry, Long> {

    Optional<DedupeEntry> findByChatIdAndDedupeKey(long chatId, String dedupeKey);
}
