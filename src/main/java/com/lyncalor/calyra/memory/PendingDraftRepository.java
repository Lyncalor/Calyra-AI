package com.lyncalor.calyra.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingDraftRepository extends JpaRepository<PendingDraft, Long> {

    Optional<PendingDraft> findByChatId(long chatId);
}
