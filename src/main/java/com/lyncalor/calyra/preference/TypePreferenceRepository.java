package com.lyncalor.calyra.preference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TypePreferenceRepository extends JpaRepository<TypePreference, Long> {

    Optional<TypePreference> findByChatIdAndType(long chatId, String type);

    List<TypePreference> findAllByChatId(long chatId);

    void deleteAllByChatId(long chatId);
}
