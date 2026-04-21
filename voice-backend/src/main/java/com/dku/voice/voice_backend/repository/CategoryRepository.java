package com.dku.voice.voice_backend.repository;

import com.dku.voice.voice_backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}