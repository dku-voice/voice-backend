package com.dku.voice.voice_backend.repository;

import com.dku.voice.voice_backend.entity.MenuOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuOptionRepository extends JpaRepository<MenuOption, Long> {
}