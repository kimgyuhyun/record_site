package com.recordsite.backend.repository;

import com.recordsite.backend.entity.ChampionTipInteraction;
import com.recordsite.backend.entity.TipInteractionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChampionTipInteractionRepository extends JpaRepository<ChampionTipInteraction, Long> {

    boolean existsByTipIdAndActorKeyAndInteractionType(Long tipId, String actorKey, TipInteractionType interactionType);
}
