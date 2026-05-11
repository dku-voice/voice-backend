package com.dku.voice.voice_backend.repository;

import com.dku.voice.voice_backend.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
}