package com.lyncalor.calyra.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingSelectionRepository extends JpaRepository<PendingSelection, Long> {

    Optional<PendingSelection> findByChatId(long chatId);
}
